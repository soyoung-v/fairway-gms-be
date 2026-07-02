package com.fairwaygms.fairwaygmsbe.common.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// ADMIN이 X-Selected-Golf-Course-Id 헤더로 대상 골프장을 지정하는 API에 붙인다.
// OpenApiConfig의 OperationCustomizer가 이 어노테이션을 감지해 헤더를 Swagger에 추가한다.
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminScopeApi {
}
