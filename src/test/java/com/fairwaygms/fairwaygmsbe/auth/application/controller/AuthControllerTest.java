package com.fairwaygms.fairwaygmsbe.auth.application.controller;

import com.fairwaygms.fairwaygmsbe.auth.application.model.request.LoginRequest;
import com.fairwaygms.fairwaygmsbe.auth.application.model.response.AuthUserResponse;
import com.fairwaygms.fairwaygmsbe.auth.application.service.AuthLoginResult;
import com.fairwaygms.fairwaygmsbe.auth.application.service.AuthService;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.response.ApiResponse;
import com.fairwaygms.fairwaygmsbe.common.security.JwtCookieProvider;
import com.fairwaygms.fairwaygmsbe.common.security.JwtProperties;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setAccessTokenCookieName("at");
        jwtProperties.setRefreshTokenCookieName("rt");
        jwtProperties.setAccessTokenCookiePath("/");
        jwtProperties.setRefreshTokenCookiePath("/api/auth/token/refresh");
        jwtProperties.setAccessTokenValiditySeconds(3600);
        jwtProperties.setRefreshTokenValiditySeconds(1209600);
        jwtProperties.setCookieHttpOnly(true);
        jwtProperties.setCookieSecure(false);
        jwtProperties.setCookieSameSite("Lax");
        authController = new AuthController(authService, new JwtCookieProvider(jwtProperties));
    }

    @Test
    void loginReturnsTwoHttpOnlyCookiesWithoutTokenBody() {
        // given
        LoginRequest request = new LoginRequest("manager@test.com", "password123!");
        AuthUserResponse user = new AuthUserResponse(
                1L,
                "manager@test.com",
                "테스트 매니저",
                UserRole.MANAGER,
                10L
        );
        when(authService.login(request)).thenReturn(new AuthLoginResult(user, "access-token", "refresh-token"));

        // when
        ResponseEntity<ApiResponse<AuthUserResponse>> response = authController.login(request);

        // then
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).hasSize(2);
        assertThat(cookies.get(0)).contains("at=access-token", "HttpOnly", "SameSite=Lax");
        assertThat(cookies.get(1)).contains("rt=refresh-token", "Path=/api/auth/token/refresh", "HttpOnly");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo(user);
    }

    @Test
    void refreshReturnsRotatedCookies() {
        // given
        AuthUserResponse user = new AuthUserResponse(
                1L,
                "admin@test.com",
                "관리자",
                UserRole.ADMIN,
                null
        );
        when(authService.refresh("old-refresh-token"))
                .thenReturn(new AuthLoginResult(user, "new-access-token", "new-refresh-token"));

        // when
        ResponseEntity<ApiResponse<?>> response = authController.refresh("old-refresh-token");

        // then
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).hasSize(2);
        assertThat(cookies.get(0)).contains("at=new-access-token");
        assertThat(cookies.get(1)).contains("rt=new-refresh-token");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo(user);
    }

    @Test
    void refreshFailureDeletesCookies() {
        // given
        when(authService.refresh(null)).thenThrow(new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));

        // when
        ResponseEntity<ApiResponse<?>> response = authController.refresh(null);

        // then
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(cookies).hasSize(2);
        assertThat(cookies.get(0)).contains("at=", "Max-Age=0");
        assertThat(cookies.get(1)).contains("rt=", "Max-Age=0");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("REFRESH_TOKEN_INVALID");
    }
}
