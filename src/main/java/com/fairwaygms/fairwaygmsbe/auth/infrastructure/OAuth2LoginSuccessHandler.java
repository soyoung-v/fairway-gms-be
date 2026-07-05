package com.fairwaygms.fairwaygmsbe.auth.infrastructure;

import com.fairwaygms.fairwaygmsbe.auth.application.service.AuthLoginResult;
import com.fairwaygms.fairwaygmsbe.auth.application.service.OAuthService;
import com.fairwaygms.fairwaygmsbe.auth.domain.enums.OAuthProvider;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.security.JwtCookieProvider;
import com.fairwaygms.fairwaygmsbe.common.security.JwtProperties;
import com.fairwaygms.fairwaygmsbe.common.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

// 카카오 인증 성공 후 분기 — 기존 연동은 JWT 쿠키 로그인, 미연동은 가입용 임시 토큰 발급 (FR-113/115)
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    public static final String SIGNUP_TOKEN_COOKIE = "oauth_signup";
    public static final String SIGNUP_TOKEN_COOKIE_PATH = "/api/auth/oauth2";
    private static final long SIGNUP_TOKEN_COOKIE_MAX_AGE_SECONDS = 600;

    private final OAuthService oAuthService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtCookieProvider jwtCookieProvider;
    private final JwtProperties jwtProperties;

    @Value("${fairway.app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuthProvider provider = OAuthProvider.valueOf(
                oauthToken.getAuthorizedClientRegistrationId().toUpperCase(Locale.ROOT));
        OAuth2User oAuth2User = oauthToken.getPrincipal();
        String providerId = oAuth2User.getName();

        try {
            OAuthService.OAuthLoginOutcome outcome = oAuthService.handleProviderLogin(provider, providerId);

            switch (outcome.type()) {
                case LOGIN -> {
                    AuthLoginResult result = outcome.loginResult();
                    response.addHeader(HttpHeaders.SET_COOKIE,
                            jwtCookieProvider.createAccessTokenCookie(result.accessToken()).toString());
                    response.addHeader(HttpHeaders.SET_COOKIE,
                            jwtCookieProvider.createRefreshTokenCookie(result.refreshToken()).toString());
                    redirect(response, "login");
                }
                case PENDING -> redirect(response, "pending");
                case SIGNUP_REQUIRED -> {
                    String signupToken = jwtTokenProvider.createOAuthSignupToken(
                            provider.name(), providerId, extractEmail(oAuth2User), extractNickname(oAuth2User));
                    response.addHeader(HttpHeaders.SET_COOKIE, createSignupCookie(signupToken).toString());
                    redirect(response, "signup");
                }
            }
        } catch (BusinessException e) {
            log.warn("소셜 로그인 처리 실패 — provider={}, code={}", provider, e.getErrorCode().getCode());
            redirect(response, "error");
        }
    }

    // 카카오 응답: kakao_account.email
    private String extractEmail(OAuth2User oAuth2User) {
        Map<String, Object> kakaoAccount = getKakaoAccount(oAuth2User);
        Object email = kakaoAccount.get("email");
        return email != null ? email.toString() : null;
    }

    // 카카오 응답: kakao_account.profile.nickname
    private String extractNickname(OAuth2User oAuth2User) {
        Map<String, Object> kakaoAccount = getKakaoAccount(oAuth2User);
        Object profile = kakaoAccount.get("profile");
        if (profile instanceof Map<?, ?> profileMap) {
            Object nickname = profileMap.get("nickname");
            return nickname != null ? nickname.toString() : null;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getKakaoAccount(OAuth2User oAuth2User) {
        Object kakaoAccount = oAuth2User.getAttributes().get("kakao_account");
        return kakaoAccount instanceof Map ? (Map<String, Object>) kakaoAccount : Map.of();
    }

    private ResponseCookie createSignupCookie(String signupToken) {
        return ResponseCookie.from(SIGNUP_TOKEN_COOKIE, signupToken)
                .httpOnly(jwtProperties.isCookieHttpOnly())
                .secure(jwtProperties.isCookieSecure())
                .sameSite(jwtProperties.getCookieSameSite())
                .path(SIGNUP_TOKEN_COOKIE_PATH)
                .maxAge(Duration.ofSeconds(SIGNUP_TOKEN_COOKIE_MAX_AGE_SECONDS))
                .build();
    }

    private void redirect(HttpServletResponse response, String status) throws IOException {
        response.sendRedirect(frontendUrl + "/oauth/callback?status=" + status);
    }
}
