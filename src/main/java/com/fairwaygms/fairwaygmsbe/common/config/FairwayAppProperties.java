package com.fairwaygms.fairwaygmsbe.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "fairway.app")
public class FairwayAppProperties {

    // 비밀번호 재설정 이메일 링크에 사용하는 프론트엔드 베이스 URL
    private String frontendUrl = "http://localhost:5173";
}
