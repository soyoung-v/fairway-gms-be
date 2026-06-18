package com.fairwaygms.fairwaygmsbe.auth.domain;

import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
// 리프레시 토큰 저장소
@Table(
        name = "refresh_token",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_refresh_token_token_hash", columnNames = "token_hash")
        },
        indexes = {
                @Index(name = "idx_refresh_token_expires_at", columnList = "expires_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    // 리프레시 토큰 PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 토큰 소유 사용자 ID
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 원문 대신 저장하는 토큰 해시
    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    // 토큰 만료 시각
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // 로그아웃 또는 재발급 폐기 여부
    @Column(name = "is_revoked", nullable = false)
    private Boolean isRevoked = false;

    // 소프트 삭제 여부
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    // 소프트 삭제 시각
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
