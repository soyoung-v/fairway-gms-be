package com.fairwaygms.fairwaygmsbe.notification.application.model.req;

import com.fairwaygms.fairwaygmsbe.notification.domain.enums.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterFcmTokenReq(
        @NotBlank String token,
        @NotNull DeviceType deviceType
) {}
