package com.fairwaygms.fairwaygmsbe.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// JPA Auditing 활성화 설정.
// BaseEntity의 createdAt / updatedAt 자동 입력이 이 설정 덕분에 동작한다.
// 단위 테스트는 @ExtendWith(MockitoExtension.class) 기반이므로 Spring 컨텍스트를 로드하지 않아 영향 없음.
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
