package com.fairwaygms.fairwaygmsbe.operation.application.model.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateReservationTeamReq(
        String teamName,
        @NotNull @Min(1) Integer playerCount,
        String memo,
        // 쉼표 구분 명단 (예: 홍길동,김철수)
        String playerNames
) {}
