package com.fairwaygms.fairwaygmsbe.notification.application.service;

import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import com.fairwaygms.fairwaygmsbe.notification.application.model.res.NotificationRes;
import com.fairwaygms.fairwaygmsbe.notification.domain.entity.Notification;
import com.fairwaygms.fairwaygmsbe.notification.domain.enums.NotificationType;
import com.fairwaygms.fairwaygmsbe.notification.domain.enums.ReferenceType;
import com.fairwaygms.fairwaygmsbe.notification.domain.repository.NotificationRepository;
import com.fairwaygms.fairwaygmsbe.notification.exception.NotificationErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long GOLF_COURSE_ID = 100L;

    private AuthenticatedUser auth;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository);
        auth = new AuthenticatedUser(USER_ID, UserRole.CADDY, GOLF_COURSE_ID);
    }

    @Test
    void getNotifications_페이징_목록을_반환한다() {
        // given
        Notification n = mockNotification(1L, USER_ID);
        Page<Notification> page = new PageImpl<>(List.of(n), PageRequest.of(0, 20), 1);
        when(notificationRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(
                eq(USER_ID), any())).thenReturn(page);

        // when
        Page<NotificationRes> result = notificationService.getNotifications(0, 20, auth);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).notificationId()).isEqualTo(1L);
    }

    @Test
    void getUnreadCount_미읽음_알림_수를_반환한다() {
        // given
        when(notificationRepository.countByUserIdAndIsReadFalseAndIsDeletedFalse(USER_ID))
                .thenReturn(3L);

        // when
        long count = notificationService.getUnreadCount(auth);

        // then
        assertThat(count).isEqualTo(3L);
    }

    @Test
    void markAsRead_알림을_읽음_처리한다() {
        // given
        Notification n = mockNotification(1L, USER_ID);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));

        // when
        NotificationRes result = notificationService.markAsRead(1L, auth);

        // then
        verify(n).markAsRead();
        assertThat(result.notificationId()).isEqualTo(1L);
    }

    @Test
    void markAsRead_다른_사용자_알림_접근_시_예외() {
        // given
        Notification n = mockNotification(1L, OTHER_USER_ID);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));

        // when / then
        assertThatThrownBy(() -> notificationService.markAsRead(1L, auth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(NotificationErrorCode.NOTIFICATION_ACCESS_DENIED));
    }

    @Test
    void markAsRead_존재하지_않는_알림_예외() {
        // given
        when(notificationRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> notificationService.markAsRead(999L, auth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
    }

    @Test
    void markAllAsRead_업데이트된_건수를_반환한다() {
        // given
        when(notificationRepository.markAllAsRead(USER_ID)).thenReturn(5);

        // when
        int updated = notificationService.markAllAsRead(auth);

        // then
        assertThat(updated).isEqualTo(5);
    }

    @Test
    void createNotification_알림을_저장하고_반환한다() {
        // given
        Notification saved = mockNotification(10L, USER_ID);
        when(notificationRepository.save(any())).thenReturn(saved);

        // when
        Notification result = notificationService.createNotification(
                GOLF_COURSE_ID, USER_ID,
                NotificationType.ASSIGNMENT_CONFIRMED, "배정 확정", "내용",
                1L, ReferenceType.ASSIGNMENT);

        // then
        verify(notificationRepository).save(any(Notification.class));
        assertThat(result.getId()).isEqualTo(10L);
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private Notification mockNotification(Long id, Long userId) {
        Notification n = spy(Notification.create(
                GOLF_COURSE_ID, userId,
                NotificationType.ASSIGNMENT_CONFIRMED, "제목", "내용",
                1L, ReferenceType.ASSIGNMENT));
        ReflectionTestUtils.setField(n, "id", id);
        ReflectionTestUtils.setField(n, "isDeleted", false);
        return n;
    }
}
