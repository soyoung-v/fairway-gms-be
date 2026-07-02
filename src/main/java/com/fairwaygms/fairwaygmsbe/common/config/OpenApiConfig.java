package com.fairwaygms.fairwaygmsbe.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

@Configuration
public class OpenApiConfig {

    private static final String COOKIE_AUTH = "CookieAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fairway GMS API")
                        .description("골프장 캐디 관리 시스템 API")
                        .version("v1.0"))
                .addSecurityItem(new SecurityRequirement().addList(COOKIE_AUTH))
                .components(new Components()
                        .addSecuritySchemes(COOKIE_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("at")
                                .description("JWT Access Token (HttpOnly 쿠키 `at`). " +
                                        "로그인 성공 시 Set-Cookie 헤더로 자동 설정된다.")));
    }

    // @AdminScopeApi 가 붙은 컨트롤러/메서드에만 X-Selected-Golf-Course-Id 헤더를 추가한다.
    @Bean
    public OperationCustomizer adminScopeHeaderCustomizer() {
        return (operation, handlerMethod) -> {
            if (isAdminScopeApi(handlerMethod)) {
                operation.addParametersItem(new Parameter()
                        .in("header")
                        .name("X-Selected-Golf-Course-Id")
                        .description("ADMIN이 대상 골프장을 지정할 때 사용. MANAGER·CADDY 요청에서는 무시된다.")
                        .required(false)
                        .schema(new StringSchema()));
            }
            return operation;
        };
    }

    private boolean isAdminScopeApi(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(AdminScopeApi.class)
                || handlerMethod.getBeanType().isAnnotationPresent(AdminScopeApi.class);
    }
}
