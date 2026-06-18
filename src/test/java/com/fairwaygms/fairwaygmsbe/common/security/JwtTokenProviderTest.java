package com.fairwaygms.fairwaygmsbe.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-key-for-jwt-provider-unit-test-at-least-32-bytes");
        properties.setAccessTokenValiditySeconds(3600);
        properties.setRefreshTokenValiditySeconds(1209600);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        jwtTokenProvider = new JwtTokenProvider(properties, environment);
    }

    @Test
    void createAccessTokenAndReadClaims() {
        // given
        Long userId = 1L;
        Long golfCourseId = 10L;

        // when
        String token = jwtTokenProvider.createAccessToken(userId, UserRole.MANAGER, golfCourseId);

        // then
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.isAccessToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(userId);
        assertThat(jwtTokenProvider.getRole(token)).isEqualTo(UserRole.MANAGER);
        assertThat(jwtTokenProvider.getGolfCourseId(token)).isEqualTo(golfCourseId);
    }

    @Test
    void validateTokenReturnsFalseWhenTokenIsInvalid() {
        // given
        String invalidToken = "invalid.jwt.token";

        // when
        boolean result = jwtTokenProvider.validateToken(invalidToken);

        // then
        assertThat(result).isFalse();
    }
}
