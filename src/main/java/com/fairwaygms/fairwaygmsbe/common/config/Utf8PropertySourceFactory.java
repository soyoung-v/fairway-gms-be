package com.fairwaygms.fairwaygmsbe.common.config;

import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

// @PropertySource의 기본 로더는 ISO-8859-1을 사용해 한글이 깨진다.
// 이 팩토리는 UTF-8로 명시적 읽기를 강제한다.
public class Utf8PropertySourceFactory implements PropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
        Properties props = new Properties();
        try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            props.load(reader);
        }
        String sourceName = (name != null && !name.isEmpty()) ? name : resource.getResource().getFilename();
        return new PropertiesPropertySource(sourceName, props);
    }
}
