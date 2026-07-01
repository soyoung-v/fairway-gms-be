package com.fairwaygms.fairwaygmsbe.settlement.application.model.req;

import com.fairwaygms.fairwaygmsbe.settlement.domain.enums.HalfBackType;
import com.fairwaygms.fairwaygmsbe.settlement.domain.enums.NoShowPolicy;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpsertFeePolicyReq(
        @NotNull @Positive BigDecimal fullRoundFee,
        BigDecimal halfRoundFee,
        HalfBackType halfBackType,
        @NotNull NoShowPolicy noShowPolicy,
        BigDecimal noShowFee
) {}
