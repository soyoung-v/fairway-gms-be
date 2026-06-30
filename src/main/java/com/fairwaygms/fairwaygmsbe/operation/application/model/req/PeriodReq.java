package com.fairwaygms.fairwaygmsbe.operation.application.model.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record PeriodReq(
        @NotNull Long courseId,
        @NotNull Integer periodNumber,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull @Min(1) Integer teeTimeInterval
) {}
