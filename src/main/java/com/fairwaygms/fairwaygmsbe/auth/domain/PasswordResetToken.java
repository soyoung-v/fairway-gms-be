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
// 비밀번호 재설정 토큰 저장소
@Table(
        name = "password_reset_token",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_password_reset_token_token_hash", columnNames = "token_hash")
        },
        indexes = {
                @Index(name = "idx_password_reset_token_expires_at", columnList = "expires_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken extends BaseEntity {

    // 비밀번호 재설정 토큰 PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 토큰 소유 사용자 ID
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 원문 대신 저장하는 재설정 토큰 해시
    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    // 토큰 만료 시각
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // 재설정 링크 사용 완료 여부
    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    // 소프트 삭제 여부
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    // 소프트 삭제 시각
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static PasswordResetToken create(Long userId, String tokenHash, LocalDateTime expiresAt) {
        PasswordResetToken token = new PasswordResetToken();
        token.userId = userId;
        token.tokenHash = tokenHash;
        token.expiresAt = expiresAt;
        token.isUsed = false;
        token.isDeleted = false;
        return token;
    }

    public void markUsed() {
        this.isUsed = true;
    }

    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}
