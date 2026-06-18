package com.fairwaygms.fairwaygmsbe.common.exception;

import com.fairwaygms.fairwaygmsbe.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 전체 컨트롤러에서 발생하는 예외를 한 곳에서 처리하고 공통 에러 응답으로 변환한다.
// 새로운 예외 유형이 필요하면 @ExceptionHandler 메서드를 여기에 추가한다.
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 비즈니스 규칙 위반 예외 처리 — Service 계층에서 throw한 BusinessException을 받아 응답으로 변환
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("BusinessException: code={}, message={}", errorCode.getCode(), e.getMessage());
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.fail(errorCode.getCode(), e.getMessage()));
    }

    // @Valid 유효성 검증 실패 처리 — 첫 번째 필드 오류 메시지를 공통 응답으로 반환한다.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse(ErrorCode.INVALID_REQUEST.getMessage());
        log.warn("ValidationException: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.INVALID_REQUEST.getCode(), message));
    }

    // 필수 헤더가 없는 경우 처리 — 예: X-Selected-Golf-Course-Id 헤더 누락
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeader(MissingRequestHeaderException e) {
        log.warn("MissingHeader: {}", e.getHeaderName());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.INVALID_REQUEST.getCode(),
                        "필수 헤더가 누락되었습니다: " + e.getHeaderName()));
    }

    // 그 외 처리되지 않은 모든 예외 — 예상치 못한 서버 오류
    // 상세 내용은 로그에만 남기고, 클라이언트에는 일반 메시지를 반환한다
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("UnhandledException", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(
                        ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                        ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}
