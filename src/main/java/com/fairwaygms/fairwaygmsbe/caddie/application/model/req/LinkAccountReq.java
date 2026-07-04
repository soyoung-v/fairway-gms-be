package com.fairwaygms.fairwaygmsbe.caddie.application.model.req;

import jakarta.validation.constraints.NotNull;

// API-306: 캐디-계정 연동 요청 (FR-306)
public record LinkAccountReq(
        @NotNull Long userId
) {
}
