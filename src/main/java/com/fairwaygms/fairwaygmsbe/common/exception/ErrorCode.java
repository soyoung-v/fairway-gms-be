package com.fairwaygms.fairwaygmsbe.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// 프로젝트 전체에서 사용하는 에러 코드 목록.
// 각 도메인별 상세 코드는 이 파일에 카테고리별로 추가한다.
@Getter
public enum ErrorCode {

    // ── 공통 ──────────────────────────────────────────────────────────────────

    // 잘못된 요청 파라미터 (유효성 검증 실패 등)
    INVALID_REQUEST("INVALID_REQUEST", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),

    // JWT 토큰 없이 보호된 API에 접근한 경우
    UNAUTHORIZED("UNAUTHORIZED", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),

    // Access Token이 만료된 경우 — 클라이언트는 Refresh Token으로 재발급 요청해야 한다
    TOKEN_EXPIRED("TOKEN_EXPIRED", "토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),

    // Refresh Token이 유효하지 않은 경우 (변조 등)
    REFRESH_TOKEN_INVALID("REFRESH_TOKEN_INVALID", "유효하지 않은 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED),

    // Refresh Token이 만료된 경우 — 클라이언트는 재로그인해야 한다
    REFRESH_TOKEN_EXPIRED("REFRESH_TOKEN_EXPIRED", "리프레시 토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),

    // 권한이 없는 API에 접근한 경우 (예: CADDY가 MANAGER 전용 API 호출)
    FORBIDDEN("FORBIDDEN", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),

    // 요청한 리소스(엔티티)를 DB에서 찾지 못한 경우
    NOT_FOUND("NOT_FOUND", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 예상치 못한 서버 내부 오류
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // ── 골프장 컨텍스트 ────────────────────────────────────────────────────────

    // ADMIN이 골프장 범위 API를 호출할 때 X-Selected-Golf-Course-Id 헤더가 없는 경우
    GOLF_COURSE_REQUIRED("GOLF_COURSE_REQUIRED", "골프장 선택이 필요합니다.", HttpStatus.BAD_REQUEST),

    // 현재 사용자가 해당 골프장에 접근할 수 없는 경우 (다른 골프장 소속 등)
    GOLF_COURSE_FORBIDDEN("GOLF_COURSE_FORBIDDEN", "해당 골프장에 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),

    // 선택하거나 참조한 골프장 ID가 DB에 없는 경우
    GOLF_COURSE_NOT_FOUND("GOLF_COURSE_NOT_FOUND", "골프장을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // ── 사용자 / 계정 ──────────────────────────────────────────────────────────

    // 해당 userId에 해당하는 사용자가 없는 경우
    USER_NOT_FOUND("USER_NOT_FOUND", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 회원가입 시 이미 사용 중인 이메일인 경우
    EMAIL_DUPLICATED("EMAIL_DUPLICATED", "이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT),

    // 로그인 시 이메일 또는 비밀번호가 틀린 경우
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),

    // 계정이 잠긴 상태 (여러 번 로그인 실패 등)
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", "잠긴 계정입니다.", HttpStatus.LOCKED),

    // 가입 승인 대기(PENDING) 상태라 로그인 또는 기능 사용이 제한된 경우
    ACCOUNT_PENDING("ACCOUNT_PENDING", "승인 대기 중인 계정입니다.", HttpStatus.FORBIDDEN),

    // 탈퇴 처리(WITHDRAWN)된 계정인 경우
    ACCOUNT_WITHDRAWN("ACCOUNT_WITHDRAWN", "탈퇴 처리된 계정입니다.", HttpStatus.FORBIDDEN),

    // 비밀번호가 정책(8~30자, 영문·숫자·특수문자 포함, 공백 불가)을 충족하지 않는 경우
    INVALID_PASSWORD("INVALID_PASSWORD", "비밀번호 정책을 충족하지 않습니다.", HttpStatus.BAD_REQUEST),

    // 이미 승인 또는 거절 처리된 가입 요청에 중복 처리 시도한 경우
    ALREADY_PROCESSED("ALREADY_PROCESSED", "이미 처리된 요청입니다.", HttpStatus.CONFLICT),

    // 유효하지 않은 역할 문자열이 요청에 포함된 경우
    INVALID_ROLE("INVALID_ROLE", "유효하지 않은 역할입니다.", HttpStatus.BAD_REQUEST),

    // 비밀번호 재설정 토큰이 만료되었거나 이미 사용된 경우
    INVALID_TOKEN("INVALID_TOKEN", "유효하지 않은 토큰입니다.", HttpStatus.BAD_REQUEST),

    // 소셜 로그인 완료 단계에서 소셜 인증 상태가 유효하지 않은 경우
    OAUTH_STATE_INVALID("OAUTH_STATE_INVALID", "소셜 로그인 상태가 유효하지 않습니다.", HttpStatus.BAD_REQUEST);

    // 클라이언트에 전달되는 에러 식별 코드
    private final String code;

    // 사용자에게 보여줄 메시지
    private final String message;

    // 이 에러에 해당하는 HTTP 응답 상태 코드
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
