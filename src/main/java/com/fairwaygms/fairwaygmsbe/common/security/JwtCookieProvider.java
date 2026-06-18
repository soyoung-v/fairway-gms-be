package com.fairwaygms.fairwaygmsbe.common.security;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class JwtCookieProvider {

    private final JwtProperties jwtProperties;

    public JwtCookieProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    // Access Token HttpOnly 쿠키 생성
    public ResponseCookie createAccessTokenCookie(String accessToken) {
        return createTokenCookie(
                jwtProperties.getAccessTokenCookieName(),
                accessToken,
                jwtProperties.getAccessTokenCookiePath(),
                jwtProperties.getAccessTokenValiditySeconds()
        );
    }

    // Refresh Token HttpOnly 쿠키 생성
    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return createTokenCookie(
                jwtProperties.getRefreshTokenCookieName(),
                refreshToken,
                jwtProperties.getRefreshTokenCookiePath(),
                jwtProperties.getRefreshTokenValiditySeconds()
        );
    }

    // Access Token 쿠키 삭제
    public ResponseCookie deleteAccessTokenCookie() {
        return deleteTokenCookie(
                jwtProperties.getAccessTokenCookieName(),
                jwtProperties.getAccessTokenCookiePath()
        );
    }

    // Refresh Token 쿠키 삭제
    public ResponseCookie deleteRefreshTokenCookie() {
        return deleteTokenCookie(
                jwtProperties.getRefreshTokenCookieName(),
                jwtProperties.getRefreshTokenCookiePath()
        );
    }

    // 공통 토큰 쿠키 생성
    private ResponseCookie createTokenCookie(String name, String value, String path, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(jwtProperties.isCookieHttpOnly())
                .secure(jwtProperties.isCookieSecure())
                .sameSite(jwtProperties.getCookieSameSite())
                .path(path)
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
    }

    // 공통 토큰 쿠키 삭제
    private ResponseCookie deleteTokenCookie(String name, String path) {
        return ResponseCookie.from(name, "")
                .httpOnly(jwtProperties.isCookieHttpOnly())
                .secure(jwtProperties.isCookieSecure())
                .sameSite(jwtProperties.getCookieSameSite())
                .path(path)
                .maxAge(Duration.ZERO)
                .build();
    }
}
