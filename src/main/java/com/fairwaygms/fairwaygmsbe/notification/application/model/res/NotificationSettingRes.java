package com.fairwaygms.fairwaygmsbe.notification.application.model.res;

import com.fairwaygms.fairwaygmsbe.notification.domain.entity.NotificationSetting;

public record NotificationSettingRes(
        Boolean isAssignmentEnabled,
        Boolean isBoardEnabled,
        Boolean isSwapEnabled
) {
    public static NotificationSettingRes from(NotificationSetting s) {
        return new NotificationSettingRes(
                s.getIsAssignmentEnabled(),
                s.getIsBoardEnabled(),
                s.getIsSwapEnabled()
        );
    }
}
