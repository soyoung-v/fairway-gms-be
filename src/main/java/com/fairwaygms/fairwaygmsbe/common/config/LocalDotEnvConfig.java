package com.fairwaygms.fairwaygmsbe.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

// local 프로파일에서만 .env 파일을 UTF-8로 로딩한다.
@Configuration
@Profile("local")
@PropertySource(
        value = "file:.env",
        factory = Utf8PropertySourceFactory.class,
        ignoreResourceNotFound = true
)
public class LocalDotEnvConfig {
}
