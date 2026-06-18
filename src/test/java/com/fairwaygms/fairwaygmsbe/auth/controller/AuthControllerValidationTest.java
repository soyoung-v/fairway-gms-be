package com.fairwaygms.fairwaygmsbe.auth.controller;

import com.fairwaygms.fairwaygmsbe.auth.service.AuthService;
import com.fairwaygms.fairwaygmsbe.common.exception.GlobalExceptionHandler;
import com.fairwaygms.fairwaygmsbe.common.security.JwtCookieProvider;
import com.fairwaygms.fairwaygmsbe.common.security.JwtProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerValidationTest {

    @Mock
    private AuthService authService;

    private LocalValidatorFactoryBean validator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        AuthController authController = new AuthController(authService, new JwtCookieProvider(createJwtProperties()));
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void signupInvalidEmailReturnsCommonValidationResponse() throws Exception {
        // expect
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "manager-test.com",
                                  "password": "password123!",
                                  "name": "테스트 매니저",
                                  "phone": "010-1234-5678",
                                  "role": "MANAGER",
                                  "golfCourseId": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.message").value("올바른 이메일 형식이 아닙니다."));
    }

    @Test
    void loginInvalidEmailReturnsCommonValidationResponse() throws Exception {
        // expect
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "manager-test.com",
                                  "password": "password123!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.message").value("올바른 이메일 형식이 아닙니다."));
    }

    private JwtProperties createJwtProperties() {
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
        return jwtProperties;
    }
}
