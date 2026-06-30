package com.fairwaygms.fairwaygmsbe.operation.application.model.req;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateSpecialDayReq(
        @NotNull LocalDate operationDate,
        String note
) {}
