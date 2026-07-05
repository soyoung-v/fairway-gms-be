package com.fairwaygms.fairwaygmsbe.auth.domain.entity;

import com.fairwaygms.fairwaygmsbe.auth.domain.enums.OAuthProvider;
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OAuthProvider provider;

    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    @Column(length = 255)
    private String email;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 소셜 가입 완료 시 user와 provider 계정 연결 (FR-115)
    public static OAuthAccount create(Long userId, OAuthProvider provider, String providerId, String email) {
        OAuthAccount account = new OAuthAccount();
        account.userId = userId;
        account.provider = provider;
        account.providerId = providerId;
        account.email = email;
        account.isDeleted = false;
        return account;
    }
}
