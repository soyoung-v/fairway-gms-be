package com.fairwaygms.fairwaygmsbe.notification.application.model.req;

import jakarta.validation.constraints.NotBlank;

public record DeleteFcmTokenReq(
        @NotBlank String token
) {}
