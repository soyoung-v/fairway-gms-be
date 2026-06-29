package com.fairwaygms.fairwaygmsbe.caddie.application.model.req;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record InitializeQueueReq(
        @NotNull
        LocalDate queueDate
) {}
