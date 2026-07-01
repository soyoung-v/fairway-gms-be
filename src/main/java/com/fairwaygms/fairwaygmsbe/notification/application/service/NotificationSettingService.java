package com.fairwaygms.fairwaygmsbe.notification.application.service;

import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.notification.application.model.req.UpdateNotificationSettingReq;
import com.fairwaygms.fairwaygmsbe.notification.application.model.res.NotificationSettingRes;
import com.fairwaygms.fairwaygmsbe.notification.domain.entity.NotificationSetting;
import com.fairwaygms.fairwaygmsbe.notification.domain.repository.NotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationSettingService {

    private final NotificationSettingRepository notificationSettingRepository;

    @Transactional(readOnly = true)
    public NotificationSettingRes getSettings(AuthenticatedUser auth) {
        NotificationSetting setting = getOrCreateDefault(auth.getUserId(), auth.getGolfCourseId());
        return NotificationSettingRes.from(setting);
    }

    @Transactional
    public NotificationSettingRes updateSettings(UpdateNotificationSettingReq req, AuthenticatedUser auth) {
        NotificationSetting setting = getOrCreateDefault(auth.getUserId(), auth.getGolfCourseId());
        setting.update(req.isAssignmentEnabled(), req.isBoardEnabled(), req.isSwapEnabled());
        return NotificationSettingRes.from(setting);
    }

    // 설정이 없으면 기본값으로 생성하고 반환한다
    @Transactional
    public NotificationSetting getOrCreateDefault(Long userId, Long golfCourseId) {
        return notificationSettingRepository
                .findByUserIdAndIsDeletedFalse(userId)
                .orElseGet(() -> notificationSettingRepository.save(
                        NotificationSetting.createDefault(userId, golfCourseId)));
    }
}
