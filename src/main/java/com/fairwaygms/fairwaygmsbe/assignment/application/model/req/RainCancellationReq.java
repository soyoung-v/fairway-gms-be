package com.fairwaygms.fairwaygmsbe.assignment.application.model.req;

import jakarta.validation.constraints.NotNull;

// API-508: 우천취소 반영 요청 (FR-513/514)
public record RainCancellationReq(
        @NotNull Long reservationTeamId
) {
}
