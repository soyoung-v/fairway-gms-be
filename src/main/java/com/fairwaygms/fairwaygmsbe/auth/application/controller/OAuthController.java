package com.fairwaygms.fairwaygmsbe.auth.application.controller;

import com.fairwaygms.fairwaygmsbe.auth.application.model.req.OAuthCompleteReq;
import com.fairwaygms.fairwaygmsbe.auth.application.model.res.OAuthCompleteRes;
import com.fairwaygms.fairwaygmsbe.auth.application.service.OAuthService;
import com.fairwaygms.fairwaygmsbe.auth.exception.AuthErrorCode;
import com.fairwaygms.fairwaygmsbe.auth.infrastructure.OAuth2LoginSuccessHandler;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/oauth2")
public class OAuthController {

    private final OAuthService oAuthService;

    // API-115: 소셜 최초 가입 완료 (FR-115) — 카카오 인증 시 발급된 임시 쿠키 기반
    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<OAuthCompleteRes>> completeSignup(
            @CookieValue(name = OAuth2LoginSuccessHandler.SIGNUP_TOKEN_COOKIE, required = false) String signupToken,
            @Valid @RequestBody OAuthCompleteReq request
    ) {
        if (signupToken == null || signupToken.isBlank()) {
            throw new BusinessException(AuthErrorCode.OAUTH_STATE_INVALID);
        }

        OAuthCompleteRes result = oAuthService.completeSignup(signupToken, request);

        // 가입 완료 후 임시 쿠키 제거
        ResponseCookie deleteCookie = ResponseCookie
                .from(OAuth2LoginSuccessHandler.SIGNUP_TOKEN_COOKIE, "")
                .path(OAuth2LoginSuccessHandler.SIGNUP_TOKEN_COOKIE_PATH)
                .maxAge(0)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(headers)
                .body(ApiResponse.success(result));
    }
}
