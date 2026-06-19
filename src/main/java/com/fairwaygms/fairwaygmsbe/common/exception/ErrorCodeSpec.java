package com.fairwaygms.fairwaygmsbe.common.exception;

import org.springframework.http.HttpStatus;

// 공통/도메인 에러코드 enum이 구현하는 인터페이스.
// BusinessException은 이 인터페이스를 통해 공통/도메인 에러코드를 모두 수용한다.
public interface ErrorCodeSpec {
    String getCode();
    String getMessage();
    HttpStatus getHttpStatus();
}
