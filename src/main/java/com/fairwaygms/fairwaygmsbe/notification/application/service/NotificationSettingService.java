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

    // 최초 조회 시 기본 설정을 lazy 생성하므로 쓰기 트랜잭션이어야 한다 (readOnly면 INSERT가 막혀 500)
    @Transactional
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
