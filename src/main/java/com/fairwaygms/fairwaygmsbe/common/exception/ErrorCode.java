package com.fairwaygms.fairwaygmsbe.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// 공통 에러코드. 도메인 전용 에러코드는 각 도메인의 exception 패키지에 정의한다.
@Getter
public enum ErrorCode implements ErrorCodeSpec {

    // ── 공통 ──────────────────────────────────────────────────────────────────

    INVALID_REQUEST("INVALID_REQUEST", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("UNAUTHORIZED", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("TOKEN_EXPIRED", "토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_INVALID("REFRESH_TOKEN_INVALID", "유효하지 않은 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED("REFRESH_TOKEN_EXPIRED", "리프레시 토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_REVOKED("REFRESH_TOKEN_REVOKED", "폐기된 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("FORBIDDEN", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    NOT_FOUND("NOT_FOUND", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // ── 골프장 컨텍스트 ────────────────────────────────────────────────────────

    // ADMIN이 골프장 범위 API 호출 시 X-Selected-Golf-Course-Id 헤더가 없는 경우
    GOLF_COURSE_REQUIRED("GOLF_COURSE_REQUIRED", "골프장 선택이 필요합니다.", HttpStatus.BAD_REQUEST),
    GOLF_COURSE_FORBIDDEN("GOLF_COURSE_FORBIDDEN", "해당 골프장에 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    GOLF_COURSE_NOT_FOUND("GOLF_COURSE_NOT_FOUND", "골프장을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
