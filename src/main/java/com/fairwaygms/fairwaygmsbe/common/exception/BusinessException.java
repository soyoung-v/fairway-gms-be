package com.fairwaygms.fairwaygmsbe.common.exception;

import lombok.Getter;

// 비즈니스 규칙 위반 시 던지는 예외. ErrorCode와 세트로 사용한다.
// 이 예외는 GlobalExceptionHandler에서 잡아 적절한 HTTP 응답으로 변환된다.
@Getter
public class BusinessException extends RuntimeException {

    // 어떤 종류의 에러인지 식별하는 코드 (HTTP 상태, 메시지 포함)
    private final ErrorCode errorCode;

    // ErrorCode에 정의된 기본 메시지를 그대로 사용할 때
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    // 기본 메시지 대신 상황에 맞는 구체적인 메시지를 전달할 때
    // 예: 어떤 ID가 없는지 명시하고 싶을 때 사용
    public BusinessException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }
}
