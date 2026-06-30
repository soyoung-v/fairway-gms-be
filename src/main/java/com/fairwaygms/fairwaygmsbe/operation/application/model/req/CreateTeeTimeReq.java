package com.fairwaygms.fairwaygmsbe.operation.application.model.req;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateTeeTimeReq(
        @NotNull Long courseId,
        @NotNull LocalDate playDate,
        @NotNull LocalTime startTime,
        @NotNull Integer periodNumber
) {}
