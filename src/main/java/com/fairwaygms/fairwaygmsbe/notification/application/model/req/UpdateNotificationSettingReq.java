package com.fairwaygms.fairwaygmsbe.notification.application.model.req;

public record UpdateNotificationSettingReq(
        Boolean isAssignmentEnabled,
        Boolean isBoardEnabled,
        Boolean isSwapEnabled
) {}
