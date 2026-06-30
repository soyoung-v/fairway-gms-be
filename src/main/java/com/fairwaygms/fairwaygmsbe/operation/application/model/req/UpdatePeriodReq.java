package com.fairwaygms.fairwaygmsbe.operation.application.model.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record UpdatePeriodReq(
        @NotNull Long periodId,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull @Min(1) Integer teeTimeInterval,
        @NotNull Boolean isActive
) {}
