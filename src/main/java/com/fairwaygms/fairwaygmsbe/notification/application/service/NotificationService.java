package com.fairwaygms.fairwaygmsbe.notification.application.service;

import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.notification.application.model.res.NotificationRes;
import com.fairwaygms.fairwaygmsbe.notification.domain.entity.Notification;
import com.fairwaygms.fairwaygmsbe.notification.domain.enums.NotificationType;
import com.fairwaygms.fairwaygmsbe.notification.domain.enums.ReferenceType;
import com.fairwaygms.fairwaygmsbe.notification.domain.repository.NotificationRepository;
import com.fairwaygms.fairwaygmsbe.notification.exception.NotificationErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public Page<NotificationRes> getNotifications(int page, int size, AuthenticatedUser auth) {
        return notificationRepository
                .findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(
                        auth.getUserId(), PageRequest.of(page, size))
                .map(NotificationRes::from);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(AuthenticatedUser auth) {
        return notificationRepository.countByUserIdAndIsReadFalseAndIsDeletedFalse(auth.getUserId());
    }

    @Transactional
    public NotificationRes markAsRead(Long notificationId, AuthenticatedUser auth) {
        Notification notification = notificationRepository.findById(notificationId)
                .filter(n -> !n.getIsDeleted())
                .orElseThrow(() -> new BusinessException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUserId().equals(auth.getUserId())) {
            throw new BusinessException(NotificationErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        notification.markAsRead();
        return NotificationRes.from(notification);
    }

    @Transactional
    public int markAllAsRead(AuthenticatedUser auth) {
        return notificationRepository.markAllAsRead(auth.getUserId());
    }

    // 도메인 이벤트 리스너에서 내부 알림 생성 시 사용
    @Transactional
    public Notification createNotification(Long golfCourseId, Long userId,
                                           NotificationType type, String title, String content,
                                           Long referenceId, ReferenceType referenceType) {
        Notification notification = Notification.create(
                golfCourseId, userId, type, title, content, referenceId, referenceType);
        return notificationRepository.save(notification);
    }
}
