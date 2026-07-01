package com.fairwaygms.fairwaygmsbe.assignment.application.model.req;

import jakarta.validation.constraints.NotNull;

public record ManualPreAssignReq(
        @NotNull Long reservationTeamId,
        @NotNull Long caddieId,
        // 지정 캐디 여부 — true이면 자동배정 풀에서 제외되고 Manager만 해제 가능
        boolean isLocked,
        // 하프백 여부 — 1인 2팀 담당 (FR-506)
        boolean isHalfBack
) {}
