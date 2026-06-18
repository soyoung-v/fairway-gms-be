package com.fairwaygms.fairwaygmsbe.common.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// JPA Auditing 활성화 설정.
// BaseEntity의 createdAt / updatedAt 자동 입력이 이 설정 덕분에 동작한다.
// @ConditionalOnSingleCandidate: 실제 DB가 연결된 경우에만 활성화된다.
// 이 조건이 없으면 DB가 없는 테스트 환경에서 빈 생성 오류가 발생한다.
@Configuration
@ConditionalOnSingleCandidate(EntityManagerFactory.class)
@EnableJpaAuditing
public class JpaConfig {
}
