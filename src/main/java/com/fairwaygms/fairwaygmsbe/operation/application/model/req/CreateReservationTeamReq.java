package com.fairwaygms.fairwaygmsbe.operation.application.model.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateReservationTeamReq(
        @NotNull Long teeTimeId,
        String teamName,
        String bookerName,
        @NotNull @Min(1) Integer playerCount,
        String memo
) {}
