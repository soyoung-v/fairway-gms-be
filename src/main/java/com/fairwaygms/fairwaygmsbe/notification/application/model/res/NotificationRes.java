package com.fairwaygms.fairwaygmsbe.notification.application.model.res;

import com.fairwaygms.fairwaygmsbe.notification.domain.entity.Notification;

import java.time.LocalDateTime;

public record NotificationRes(
        Long notificationId,
        String type,
        String title,
        String content,
        Boolean isRead,
        Long referenceId,
        String referenceType,
        LocalDateTime createdAt
) {
    public static NotificationRes from(Notification n) {
        return new NotificationRes(
                n.getId(),
                n.getType().name(),
                n.getTitle(),
                n.getContent(),
                n.getIsRead(),
                n.getReferenceId(),
                n.getReferenceType() != null ? n.getReferenceType().name() : null,
                n.getCreatedAt()
        );
    }
}
