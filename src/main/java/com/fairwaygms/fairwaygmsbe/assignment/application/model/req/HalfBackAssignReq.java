package com.fairwaygms.fairwaygmsbe.assignment.application.model.req;

import jakarta.validation.constraints.NotNull;

// API-502: 하프백(투근무) 배정 요청 (FR-506)
public record HalfBackAssignReq(
        @NotNull Long caddieId,
        @NotNull Long reservationTeamId1,
        @NotNull Long reservationTeamId2,
        String reason
) {
}
