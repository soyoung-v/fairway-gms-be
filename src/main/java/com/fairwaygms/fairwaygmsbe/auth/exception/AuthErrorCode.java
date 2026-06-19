package com.fairwaygms.fairwaygmsbe.auth.exception;

import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCodeSpec;
import lombok.Getter;
import org.springframework.http.HttpStatus;

// 인증/계정 도메인 전용 에러코드
@Getter
public enum AuthErrorCode implements ErrorCodeSpec {

    USER_NOT_FOUND("USER_NOT_FOUND", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    EMAIL_DUPLICATED("EMAIL_DUPLICATED", "이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", "잠긴 계정입니다.", HttpStatus.LOCKED),
    ACCOUNT_PENDING("ACCOUNT_PENDING", "승인 대기 중인 계정입니다.", HttpStatus.FORBIDDEN),
    ACCOUNT_WITHDRAWN("ACCOUNT_WITHDRAWN", "탈퇴 처리된 계정입니다.", HttpStatus.FORBIDDEN),
    INVALID_PASSWORD("INVALID_PASSWORD", "비밀번호 정책을 충족하지 않습니다.", HttpStatus.BAD_REQUEST),
    ADMIN_BOOTSTRAP_REQUIRED("ADMIN_BOOTSTRAP_REQUIRED", "초기 관리자 계정 설정이 필요합니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    ALREADY_PROCESSED("ALREADY_PROCESSED", "이미 처리된 요청입니다.", HttpStatus.CONFLICT),
    INVALID_ROLE("INVALID_ROLE", "유효하지 않은 역할입니다.", HttpStatus.BAD_REQUEST),
    INVALID_TOKEN("INVALID_TOKEN", "유효하지 않은 토큰입니다.", HttpStatus.BAD_REQUEST),
    OAUTH_STATE_INVALID("OAUTH_STATE_INVALID", "소셜 로그인 상태가 유효하지 않습니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    AuthErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
