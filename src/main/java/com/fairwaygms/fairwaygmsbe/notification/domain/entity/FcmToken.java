package com.fairwaygms.fairwaygmsbe.notification.domain.entity;

import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import com.fairwaygms.fairwaygmsbe.notification.domain.enums.DeviceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(name = "fcm_token",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_fcm_token_token", columnNames = "token")
        },
        indexes = {
                @Index(name = "idx_fcm_token_user_active", columnList = "user_id, is_active")
        })
public class FcmToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "golf_course_id", nullable = false)
    private Long golfCourseId;

    @Column(nullable = false, length = 500, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 30)
    private DeviceType deviceType;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static FcmToken create(Long userId, Long golfCourseId, String token, DeviceType deviceType) {
        FcmToken t = new FcmToken();
        t.userId = userId;
        t.golfCourseId = golfCourseId;
        t.token = token;
        t.deviceType = deviceType;
        t.isActive = true;
        t.isDeleted = false;
        return t;
    }

    public void updateLastUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.isActive = false;
    }
}
