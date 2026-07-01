package com.fairwaygms.fairwaygmsbe.settlement.application.model.res;

import com.fairwaygms.fairwaygmsbe.settlement.domain.entity.MonthlySettlement;

import java.time.LocalDateTime;

public record MonthlySettlementRes(
        Long settlementId,
        String settlementYearMonth,
        String status,
        LocalDateTime confirmedAt
) {
    public static MonthlySettlementRes from(MonthlySettlement s) {
        return new MonthlySettlementRes(
                s.getId(),
                s.getSettlementYearMonth(),
                s.getStatus().name(),
                s.getConfirmedAt()
        );
    }
}
