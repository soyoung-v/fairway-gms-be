package com.fairwaygms.fairwaygmsbe.settlement.application.model.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RainCancellationCalculateReq(
        @NotNull Long assignmentId,
        @NotNull @Min(0) Integer playedHoleCount
) {}
