package com.fairwaygms.fairwaygmsbe.notification.application.service;

import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import com.fairwaygms.fairwaygmsbe.notification.application.model.req.UpdateNotificationSettingReq;
import com.fairwaygms.fairwaygmsbe.notification.application.model.res.NotificationSettingRes;
import com.fairwaygms.fairwaygmsbe.notification.domain.entity.NotificationSetting;
import com.fairwaygms.fairwaygmsbe.notification.domain.repository.NotificationSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationSettingServiceTest {

    @Mock private NotificationSettingRepository notificationSettingRepository;

    private NotificationSettingService notificationSettingService;

    private static final Long USER_ID = 1L;
    private static final Long GOLF_COURSE_ID = 100L;

    private AuthenticatedUser auth;

    @BeforeEach
    void setUp() {
        notificationSettingService = new NotificationSettingService(notificationSettingRepository);
        auth = new AuthenticatedUser(USER_ID, UserRole.CADDY, GOLF_COURSE_ID);
    }

    @Test
    void getSettings_설정이_없으면_기본값으로_생성한다() {
        // given
        when(notificationSettingRepository.findByUserIdAndIsDeletedFalse(USER_ID))
                .thenReturn(Optional.empty());
        NotificationSetting defaultSetting = NotificationSetting.createDefault(USER_ID, GOLF_COURSE_ID);
        when(notificationSettingRepository.save(any())).thenReturn(defaultSetting);

        // when
        NotificationSettingRes result = notificationSettingService.getSettings(auth);

        // then
        verify(notificationSettingRepository).save(any(NotificationSetting.class));
        assertThat(result.isAssignmentEnabled()).isTrue();
        assertThat(result.isBoardEnabled()).isTrue();
        assertThat(result.isSwapEnabled()).isTrue();
    }

    @Test
    void updateSettings_설정_항목을_수정한다() {
        // given
        NotificationSetting existing = NotificationSetting.createDefault(USER_ID, GOLF_COURSE_ID);
        when(notificationSettingRepository.findByUserIdAndIsDeletedFalse(USER_ID))
                .thenReturn(Optional.of(existing));

        UpdateNotificationSettingReq req = new UpdateNotificationSettingReq(false, true, false);

        // when
        NotificationSettingRes result = notificationSettingService.updateSettings(req, auth);

        // then
        assertThat(result.isAssignmentEnabled()).isFalse();
        assertThat(result.isBoardEnabled()).isTrue();
        assertThat(result.isSwapEnabled()).isFalse();
    }
}
