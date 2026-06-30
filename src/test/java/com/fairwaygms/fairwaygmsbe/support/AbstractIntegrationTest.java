package com.fairwaygms.fairwaygmsbe.support;

import com.fairwaygms.fairwaygmsbe.auth.application.service.EmailService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

// 통합 테스트 공통 베이스. 로컬 MySQL을 사용하고, 각 테스트 후 트랜잭션을 롤백한다.
// local: datasource(MySQL) 제공, integration: ddl-auto/mail/oauth2/jwt 테스트 설정으로 오버라이드
@SpringBootTest
@ActiveProfiles({"local", "integration"})
@Transactional
public abstract class AbstractIntegrationTest {

    // 실제 이메일 전송 방지
    @MockitoBean
    protected EmailService emailService;
}
