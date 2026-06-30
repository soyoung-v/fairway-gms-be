package com.fairwaygms.fairwaygmsbe.operation.application.model.req;

import jakarta.validation.constraints.NotNull;

public record UpdateVipReq(
        @NotNull Boolean isVip,
        String memo
) {}
