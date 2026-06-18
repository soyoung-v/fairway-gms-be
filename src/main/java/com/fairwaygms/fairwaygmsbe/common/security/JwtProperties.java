package com.fairwaygms.fairwaygmsbe.common.security;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "fairway.jwt")
public class JwtProperties {

    private static final long DEFAULT_ACCESS_TOKEN_VALIDITY_SECONDS = 3600L;
    private static final long DEFAULT_REFRESH_TOKEN_VALIDITY_SECONDS = 1209600L;

    // JWT 서명 비밀키
    private String secret;

    // JWT 발급자
    private String issuer = "fairway-gms";

    // Access Token 유효 시간(초)
    @Min(1)
    private long accessTokenValiditySeconds = DEFAULT_ACCESS_TOKEN_VALIDITY_SECONDS;

    // Refresh Token 유효 시간(초)
    @Min(1)
    private long refreshTokenValiditySeconds = DEFAULT_REFRESH_TOKEN_VALIDITY_SECONDS;

    // Access Token 쿠키 이름
    private String accessTokenCookieName = "at";

    // Refresh Token 쿠키 이름
    private String refreshTokenCookieName = "rt";

    // Access Token 쿠키 경로
    private String accessTokenCookiePath = "/";

    // Refresh Token 쿠키 경로
    private String refreshTokenCookiePath = "/api/auth/token/refresh";

    // HTTPS 환경 전용 쿠키 여부
    private boolean cookieSecure;

    // JavaScript 접근 차단 여부
    private boolean cookieHttpOnly = true;

    // CSRF 완화를 위한 SameSite 정책
    private String cookieSameSite = "Lax";
}
