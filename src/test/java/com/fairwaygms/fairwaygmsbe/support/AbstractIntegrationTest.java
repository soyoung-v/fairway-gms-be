package com.fairwaygms.fairwaygmsbe.support;

import com.fairwaygms.fairwaygmsbe.auth.application.service.EmailService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

// 통합 테스트 공통 베이스. H2 인메모리 DB를 사용하고, 각 테스트 후 트랜잭션을 롤백한다.
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
public abstract class AbstractIntegrationTest {

    // 실제 이메일 전송 방지
    @MockitoBean
    protected EmailService emailService;
}
