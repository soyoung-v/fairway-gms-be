package com.fairwaygms.fairwaygmsbe.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import static org.assertj.core.api.Assertions.assertThat;

class JwtCookieProviderTest {

    private JwtCookieProvider jwtCookieProvider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = createJwtProperties();
        jwtCookieProvider = new JwtCookieProvider(properties);
    }

    @Test
    void createAccessTokenCookieContainsSecurityAttributes() {
        // when
        ResponseCookie cookie = jwtCookieProvider.createAccessTokenCookie("access-token");

        // then
        assertThat(cookie.getName()).isEqualTo("at");
        assertThat(cookie.getValue()).isEqualTo("access-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isFalse();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(3600);
        assertThat(cookie.toString()).contains("SameSite=Lax");
    }

    @Test
    void createRefreshTokenCookieUsesRefreshPath() {
        // when
        ResponseCookie cookie = jwtCookieProvider.createRefreshTokenCookie("refresh-token");

        // then
        assertThat(cookie.getName()).isEqualTo("rt");
        assertThat(cookie.getPath()).isEqualTo("/api/auth/token/refresh");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(1209600);
    }

    @Test
    void deleteTokenCookieExpiresCookie() {
        // when
        ResponseCookie cookie = jwtCookieProvider.deleteAccessTokenCookie();

        // then
        assertThat(cookie.getName()).isEqualTo("at");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge().getSeconds()).isZero();
    }

    private JwtProperties createJwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setAccessTokenCookieName("at");
        properties.setRefreshTokenCookieName("rt");
        properties.setAccessTokenCookiePath("/");
        properties.setRefreshTokenCookiePath("/api/auth/token/refresh");
        properties.setAccessTokenValiditySeconds(3600);
        properties.setRefreshTokenValiditySeconds(1209600);
        properties.setCookieHttpOnly(true);
        properties.setCookieSecure(false);
        properties.setCookieSameSite("Lax");
        return properties;
    }
}
