package com.fairwaygms.fairwaygmsbe.auth.infrastructure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "fairway.bootstrap.admin")
public class AdminBootstrapProperties {

    private boolean enabled;
    private String email;
    private String password;
    private String name = "관리자";
}
