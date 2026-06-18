package com.fairwaygms.fairwaygmsbe.common.config;

import com.fairwaygms.fairwaygmsbe.common.security.JwtAuthenticationFilter;
import com.fairwaygms.fairwaygmsbe.common.security.JwtProperties;
import com.fairwaygms.fairwaygmsbe.common.security.SecurityWhitelist;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    // REST API 기본 보안 필터 체인
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {
        return http
                // TODO: 쿠키 인증 프론트 연동 시 CORS와 CSRF 방어 정책을 함께 확정
                .csrf(csrf -> csrf.disable())
                // JWT 인증을 사용하므로 서버 세션을 만들지 않는다.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // HTML form 로그인 비활성화
                .formLogin(formLogin -> formLogin.disable())
                // 브라우저 기본 인증 팝업 비활성화
                .httpBasic(httpBasic -> httpBasic.disable())
                // 공개 API와 Swagger 접근 허용
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(SecurityWhitelist.PERMIT_ALL_PATHS).permitAll()
                        .anyRequest().authenticated()
                )
                // JWT 인증 필터 연결
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // 비밀번호 BCrypt 해시 인코더
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
