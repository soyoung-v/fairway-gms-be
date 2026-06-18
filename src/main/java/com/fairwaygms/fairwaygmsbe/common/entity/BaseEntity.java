package com.fairwaygms.fairwaygmsbe.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// 모든 Entity가 공통으로 가져야 하는 생성일시/수정일시 필드를 모아둔 부모 클래스.
// @MappedSuperclass: 이 클래스를 직접 테이블로 만들지 않고, 자식 Entity에 필드를 상속시킨다.
// @EntityListeners: JPA가 Entity를 저장/수정할 때 자동으로 날짜를 채워준다 (JpaConfig의 @EnableJpaAuditing과 함께 동작).
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    // 최초 저장 시점에 자동으로 채워진다. 이후 변경되지 않는다.
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 마지막으로 수정된 시점에 자동으로 갱신된다.
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
