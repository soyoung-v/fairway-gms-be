package com.fairwaygms.fairwaygmsbe.common.security;

public final class SecurityWhitelist {

    // 비로그인 접근 허용 경로
    public static final String[] PERMIT_ALL_PATHS = {
            "/api/public/**",
            "/api/auth/login",
            "/api/auth/signup",
            "/api/auth/check-email",
            "/api/auth/token/refresh",
            "/api/auth/password-reset/request",
            "/api/auth/password-reset/confirm",
            "/oauth2/**",
            "/login/oauth2/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/actuator/health"
    };

    private SecurityWhitelist() {
    }
}
