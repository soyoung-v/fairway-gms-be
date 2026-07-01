package com.fairwaygms.fairwaygmsbe.settlement.application.model.res;

public record RoundSummaryRes(
        Long caddieId,
        String caddieName,
        long totalRoundCount,
        long totalAssignmentCount
) {}
