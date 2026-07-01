package com.fairwaygms.fairwaygmsbe.settlement.application.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AdjustCaddieFeeReq(
        @NotBlank String yearMonth,
        @NotNull BigDecimal adjustedFee,
        @NotBlank String reason
) {}
