package com.fairwaygms.fairwaygmsbe.auth.infrastructure;

import com.fairwaygms.fairwaygmsbe.auth.application.service.AdminBootstrapService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private final AdminBootstrapService adminBootstrapService;

    // 애플리케이션 기동 후 초기 ADMIN 계정 필요 여부를 확인한다.
    @Override
    public void run(ApplicationArguments args) {
        adminBootstrapService.bootstrap();
    }
}
