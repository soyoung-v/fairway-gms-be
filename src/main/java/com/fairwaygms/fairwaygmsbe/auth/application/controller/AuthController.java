package com.fairwaygms.fairwaygmsbe.auth.application.controller;

import com.fairwaygms.fairwaygmsbe.auth.application.model.req.ChangePasswordReq;
import com.fairwaygms.fairwaygmsbe.auth.application.model.req.ForgotPasswordReq;
import com.fairwaygms.fairwaygmsbe.auth.application.model.req.LoginReq;
import com.fairwaygms.fairwaygmsbe.auth.application.model.req.ResetPasswordReq;
import com.fairwaygms.fairwaygmsbe.auth.application.model.req.SignupReq;
import com.fairwaygms.fairwaygmsbe.auth.application.model.res.AuthUserRes;
import com.fairwaygms.fairwaygmsbe.auth.application.model.res.CheckEmailRes;
import com.fairwaygms.fairwaygmsbe.auth.application.model.res.MeRes;
import com.fairwaygms.fairwaygmsbe.auth.application.model.res.SignupRes;
import com.fairwaygms.fairwaygmsbe.auth.application.service.AuthLoginResult;
import com.fairwaygms.fairwaygmsbe.auth.application.service.AuthService;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.response.ApiResponse;
import com.fairwaygms.fairwaygmsbe.common.response.MessageResponse;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.JwtCookieProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtCookieProvider jwtCookieProvider;

    // 회원가입 후 승인 대기 상태를 반환하고 토큰은 발급하지 않는다.
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupRes>> signup(@Valid @RequestBody SignupReq request) {
        SignupRes response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // 로그인 성공 시 JWT 원문은 body가 아니라 HttpOnly Cookie로만 내려준다.
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthUserRes>> login(@Valid @RequestBody LoginReq request) {
        AuthLoginResult result = authService.login(request);
        HttpHeaders headers = createCookieHeaders(
                jwtCookieProvider.createAccessTokenCookie(result.accessToken()),
                jwtCookieProvider.createRefreshTokenCookie(result.refreshToken())
        );
        return ResponseEntity.ok().headers(headers).body(ApiResponse.success(result.user()));
    }

    // Refresh Token 저장소는 폐기하고 브라우저 쿠키도 함께 제거한다.
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<MessageResponse>> logout(
            @CookieValue(name = "rt", required = false) String refreshToken
    ) {
        authService.logout(refreshToken);
        HttpHeaders headers = createCookieHeaders(
                jwtCookieProvider.deleteAccessTokenCookie(),
                jwtCookieProvider.deleteRefreshTokenCookie()
        );
        return ResponseEntity.ok().headers(headers).body(ApiResponse.success(new MessageResponse("로그아웃되었습니다.")));
    }

    // Refresh Token Rotation 성공 시 새 at/rt 쿠키를 내려준다.
    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<?>> refresh(
            @CookieValue(name = "rt", required = false) String refreshToken
    ) {
        try {
            AuthLoginResult result = authService.refresh(refreshToken);
            HttpHeaders headers = createCookieHeaders(
                    jwtCookieProvider.createAccessTokenCookie(result.accessToken()),
                    jwtCookieProvider.createRefreshTokenCookie(result.refreshToken())
            );
            return ResponseEntity.ok().headers(headers).body(ApiResponse.success(result.user()));
        } catch (BusinessException e) {
            if (isRefreshTokenFailure(e)) {
                HttpHeaders headers = createCookieHeaders(
                        jwtCookieProvider.deleteAccessTokenCookie(),
                        jwtCookieProvider.deleteRefreshTokenCookie()
                );
                var errorCode = e.getErrorCode();
                return ResponseEntity.status(errorCode.getHttpStatus()).headers(headers)
                        .body(ApiResponse.fail(errorCode.getCode(), e.getMessage()));
            }
            throw e;
        }
    }

    // 이메일 중복 여부를 반환한다. available=true이면 가입 가능하다.
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<CheckEmailRes>> checkEmail(
            @RequestParam @NotBlank(message = "이메일은 필수입니다.")
            @Email(message = "올바른 이메일 형식이 아닙니다.") String email
    ) {
        return ResponseEntity.ok(ApiResponse.success(CheckEmailRes.of(authService.isEmailAvailable(email))));
    }

    // 현재 비밀번호를 검증한 후 새 비밀번호로 변경한다.
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<MessageResponse>> changePassword(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody ChangePasswordReq request
    ) {
        if (authenticatedUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        authService.changePassword(authenticatedUser.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(new MessageResponse("비밀번호가 변경되었습니다.")));
    }

    // 이메일 미존재 여부는 노출하지 않고 항상 성공 응답을 반환한다.
    @PostMapping("/password-reset/request")
    public ResponseEntity<ApiResponse<MessageResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordReq request
    ) {
        authService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success(new MessageResponse("입력한 이메일로 비밀번호 재설정 링크를 발송했습니다.")));
    }

    // 토큰과 새 비밀번호를 받아 비밀번호를 재설정한다.
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponse<MessageResponse>> resetPassword(
            @Valid @RequestBody ResetPasswordReq request
    ) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(new MessageResponse("비밀번호가 재설정되었습니다.")));
    }

    // SecurityContext의 인증 사용자 기준 내 정보를 조회한다.
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeRes>> me(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(ApiResponse.success(authService.getMe(authenticatedUser.getUserId())));
    }

    // 여러 Set-Cookie 헤더를 누락 없이 응답에 추가한다.
    private HttpHeaders createCookieHeaders(ResponseCookie... cookies) {
        HttpHeaders headers = new HttpHeaders();
        for (ResponseCookie cookie : cookies) {
            headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        return headers;
    }

    // 재발급 실패는 클라이언트 쿠키를 함께 정리한다.
    private boolean isRefreshTokenFailure(BusinessException e) {
        var errorCode = e.getErrorCode();
        return errorCode == ErrorCode.REFRESH_TOKEN_INVALID
                || errorCode == ErrorCode.REFRESH_TOKEN_EXPIRED
                || errorCode == ErrorCode.REFRESH_TOKEN_REVOKED;
    }
}
