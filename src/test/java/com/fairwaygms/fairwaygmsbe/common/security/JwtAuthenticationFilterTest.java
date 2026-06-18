package com.fairwaygms.fairwaygmsbe.common.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        JwtProperties properties = createJwtProperties();
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        jwtTokenProvider = new JwtTokenProvider(properties, environment);
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenProvider, properties);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticateWithAccessTokenCookie() throws Exception {
        // given
        String token = jwtTokenProvider.createAccessToken(1L, UserRole.MANAGER, 10L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/protected");
        request.setCookies(new Cookie("at", token));
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        jwtAuthenticationFilter.doFilter(request, response, new MockFilterChain());

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(AuthenticatedUser.class);
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        assertThat(principal.getUserId()).isEqualTo(1L);
        assertThat(principal.getRole()).isEqualTo(UserRole.MANAGER);
        assertThat(principal.getGolfCourseId()).isEqualTo(10L);
    }

    @Test
    void bearerHeaderIsIgnored() throws Exception {
        // given
        String token = jwtTokenProvider.createAccessToken(1L, UserRole.MANAGER, 10L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/protected");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        jwtAuthenticationFilter.doFilter(request, response, new MockFilterChain());

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private JwtProperties createJwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-key-for-jwt-filter-unit-test-at-least-32-bytes");
        properties.setIssuer("fairway-gms");
        properties.setAccessTokenValiditySeconds(3600);
        properties.setRefreshTokenValiditySeconds(1209600);
        properties.setAccessTokenCookieName("at");
        properties.setRefreshTokenCookieName("rt");
        properties.setAccessTokenCookiePath("/");
        properties.setRefreshTokenCookiePath("/api/auth/token/refresh");
        properties.setCookieHttpOnly(true);
        properties.setCookieSecure(false);
        properties.setCookieSameSite("Lax");
        return properties;
    }
}
