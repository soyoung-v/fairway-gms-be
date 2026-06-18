package com.fairwaygms.fairwaygmsbe.auth.domain;

import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
// 소셜 계정 연동 정보
@Table(
        name = "oauth_account",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_oauth_account_provider_provider_id", columnNames = {"provider", "provider_id"})
        },
        indexes = {
                @Index(name = "idx_oauth_account_user_id", columnList = "user_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OAuthAccount extends BaseEntity {

    // 소셜 계정 PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 연동된 사용자 ID
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 소셜 로그인 제공자
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OAuthProvider provider;

    // 제공자별 고유 계정 ID
    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    // 소셜 제공자에서 받은 이메일
    @Column(length = 255)
    private String email;

    // 소프트 삭제 여부
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    // 소프트 삭제 시각
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
