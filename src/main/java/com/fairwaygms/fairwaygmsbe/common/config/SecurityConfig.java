package com.fairwaygms.fairwaygmsbe.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 비로그인 접근 허용 경로
    private static final String[] PERMIT_ALL_PATHS = {
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

    // REST API 기본 보안 필터 체인
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // REST API는 세션 쿠키 기반 CSRF 보호를 사용하지 않는다.
                .csrf(csrf -> csrf.disable())
                // JWT 도입 예정이므로 서버 세션을 만들지 않는다.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // HTML form 로그인 비활성화
                .formLogin(formLogin -> formLogin.disable())
                // 브라우저 기본 인증 팝업 비활성화
                .httpBasic(httpBasic -> httpBasic.disable())
                // 공개 API와 Swagger 접근 허용
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PERMIT_ALL_PATHS).permitAll()
                        .anyRequest().authenticated()
                )
                .build();
    }

    // 비밀번호 BCrypt 해시 인코더
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
