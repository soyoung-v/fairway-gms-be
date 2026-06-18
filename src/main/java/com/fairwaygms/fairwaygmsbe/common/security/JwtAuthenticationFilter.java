package com.fairwaygms.fairwaygmsbe.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, JwtProperties jwtProperties) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
    }

    // 공개 경로는 JWT 검증 없이 통과
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        for (String permitAllPath : SecurityWhitelist.PERMIT_ALL_PATHS) {
            if (pathMatcher.match(permitAllPath, requestUri)) {
                return true;
            }
        }
        return false;
    }

    // HttpOnly Access Token 쿠키 인증 처리
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveAccessTokenCookie(request);

        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtTokenProvider.validateToken(token) || !jwtTokenProvider.isAccessToken(token)) {
            SecurityContextHolder.clearContext();
            writeUnauthorizedResponse(response, jwtTokenProvider.isExpired(token)
                    ? ErrorCode.TOKEN_EXPIRED
                    : ErrorCode.UNAUTHORIZED);
            return;
        }

        setAuthentication(request, token);
        filterChain.doFilter(request, response);
    }

    // at 쿠키에서 Access Token 추출
    private String resolveAccessTokenCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (jwtProperties.getAccessTokenCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    // SecurityContext 인증 객체 저장
    private void setAuthentication(HttpServletRequest request, String token) {
        UserRole role = jwtTokenProvider.getRole(token);
        AuthenticatedUser principal = new AuthenticatedUser(
                jwtTokenProvider.getUserId(token),
                role,
                jwtTokenProvider.getGolfCourseId(token)
        );
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + role.name())
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // TODO: User 상태 검증은 UserDetails/AuthService 단계에서 보강한다.
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // JWT 인증 실패 응답 작성
    private void writeUnauthorizedResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(errorCode.getCode(), errorCode.getMessage()));
    }
}
