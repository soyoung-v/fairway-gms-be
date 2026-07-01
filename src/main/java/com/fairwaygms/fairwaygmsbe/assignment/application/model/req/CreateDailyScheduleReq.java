package com.fairwaygms.fairwaygmsbe.assignment.application.model.req;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateDailyScheduleReq(
        @NotNull LocalDate scheduleDate
) {}
