package com.fairwaygms.fairwaygmsbe.common.exception;

import lombok.Getter;

// 비즈니스 규칙 위반 시 던지는 예외. ErrorCodeSpec 구현체(ErrorCode, AuthErrorCode 등)와 세트로 사용한다.
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCodeSpec errorCode;

    public BusinessException(ErrorCodeSpec errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    // 기본 메시지 대신 상황에 맞는 구체적인 메시지를 전달할 때
    public BusinessException(ErrorCodeSpec errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }
}
