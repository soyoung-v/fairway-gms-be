package com.fairwaygms.fairwaygmsbe.caddie.application.model.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record AdjustQueueReq(
        @NotNull LocalDate queueDate,
        @NotNull @Positive Integer queueNumber,
        String reason
) {}
