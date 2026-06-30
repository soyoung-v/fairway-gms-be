package com.fairwaygms.fairwaygmsbe.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_GOLF_COURSE_ID = "golfCourseId";
    private static final String CLAIM_TYPE = "type";
    private static final String TOKEN_TYPE_ACCESS = "ACCESS";
    private static final String TOKEN_TYPE_REFRESH = "REFRESH";

    // local/test 전용 fallback. prod에서는 secret 환경변수가 반드시 필요하다.
    private static final String LOCAL_TEST_FALLBACK_SECRET =
            "local-test-only-fairway-gms-jwt-secret-key-at-least-32-bytes";

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties, Environment environment) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(resolveSecret(jwtProperties.getSecret(), environment)
                .getBytes(StandardCharsets.UTF_8));
    }

    // Access Token 생성
    public String createAccessToken(Long userId, UserRole role, Long golfCourseId) {
        return createToken(userId, role, golfCourseId, TOKEN_TYPE_ACCESS,
                jwtProperties.getAccessTokenValiditySeconds());
    }

    // Refresh Token 생성
    public String createRefreshToken(Long userId, UserRole role, Long golfCourseId) {
        return createToken(userId, role, golfCourseId, TOKEN_TYPE_REFRESH,
                jwtProperties.getRefreshTokenValiditySeconds());
    }

    // 토큰 서명과 만료 시간 검증
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // 토큰 만료 여부 확인
    public boolean isExpired(String token) {
        try {
            return parseClaims(token).getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // Access Token 타입 여부 확인
    public boolean isAccessToken(String token) {
        return TOKEN_TYPE_ACCESS.equals(getTokenType(token));
    }

    // Refresh Token 타입 여부 확인
    public boolean isRefreshToken(String token) {
        return TOKEN_TYPE_REFRESH.equals(getTokenType(token));
    }

    // 토큰에서 userId 추출
    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    // 토큰에서 role 추출
    public UserRole getRole(String token) {
        return UserRole.valueOf(parseClaims(token).get(CLAIM_ROLE, String.class));
    }

    // 토큰에서 golfCourseId 추출
    public Long getGolfCourseId(String token) {
        Object value = parseClaims(token).get(CLAIM_GOLF_COURSE_ID);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }

    // 토큰 타입 추출
    public String getTokenType(String token) {
        return parseClaims(token).get(CLAIM_TYPE, String.class);
    }

    // JWT Claims 파싱
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 공통 토큰 생성
    private String createToken(Long userId, UserRole role, Long golfCourseId, String tokenType, long validitySeconds) {
        Instant now = Instant.now();
        // jti(JWT ID)로 고속 연속 발급 시 동일 payload 중복을 방지한다
        JwtBuilder builder = Jwts.builder()
                .id(java.util.UUID.randomUUID().toString())
                .issuer(jwtProperties.getIssuer())
                .subject(String.valueOf(userId))
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_TYPE, tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(validitySeconds)));

        if (golfCourseId != null) {
            builder.claim(CLAIM_GOLF_COURSE_ID, golfCourseId);
        }

        return builder.signWith(secretKey, Jwts.SIG.HS256).compact();
    }

    // 프로필별 secret 결정
    private String resolveSecret(String configuredSecret, Environment environment) {
        if (StringUtils.hasText(configuredSecret)) {
            return configuredSecret;
        }

        if (isProdProfile(environment)) {
            throw new IllegalStateException("fairway.jwt.secret must be provided in prod profile.");
        }

        return LOCAL_TEST_FALLBACK_SECRET;
    }

    // prod 프로필 여부 확인
    private boolean isProdProfile(Environment environment) {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "prod".equalsIgnoreCase(profile));
    }
}
