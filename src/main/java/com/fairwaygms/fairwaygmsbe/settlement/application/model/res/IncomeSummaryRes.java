package com.fairwaygms.fairwaygmsbe.settlement.application.model.res;

import java.math.BigDecimal;

public record IncomeSummaryRes(
        Long caddieId,
        String caddieName,
        BigDecimal totalFee,
        BigDecimal adjustedFee
) {}
