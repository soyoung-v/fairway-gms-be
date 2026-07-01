package com.fairwaygms.fairwaygmsbe.notification.domain.entity;

import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import com.fairwaygms.fairwaygmsbe.notification.domain.enums.NotificationType;
import com.fairwaygms.fairwaygmsbe.notification.domain.enums.ReferenceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(name = "notification", indexes = {
        @Index(name = "idx_notification_golf_course", columnList = "golf_course_id"),
        @Index(name = "idx_notification_user_is_read", columnList = "user_id, is_read"),
        @Index(name = "idx_notification_created_at", columnList = "created_at")
})
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "golf_course_id", nullable = false)
    private Long golfCourseId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "reference_id")
    private Long referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", length = 30)
    private ReferenceType referenceType;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static Notification create(Long golfCourseId, Long userId, NotificationType type,
                                      String title, String content,
                                      Long referenceId, ReferenceType referenceType) {
        Notification n = new Notification();
        n.golfCourseId = golfCourseId;
        n.userId = userId;
        n.type = type;
        n.title = title;
        n.content = content;
        n.referenceId = referenceId;
        n.referenceType = referenceType;
        n.isRead = false;
        n.isDeleted = false;
        return n;
    }

    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
