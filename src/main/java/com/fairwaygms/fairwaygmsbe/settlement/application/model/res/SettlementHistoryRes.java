package com.fairwaygms.fairwaygmsbe.settlement.application.model.res;

import com.fairwaygms.fairwaygmsbe.settlement.domain.entity.SettlementChangeHistory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SettlementHistoryRes(
        Long historyId,
        String changeType,
        BigDecimal beforeAmount,
        BigDecimal afterAmount,
        String reason,
        LocalDateTime createdAt
) {
    public static SettlementHistoryRes from(SettlementChangeHistory h) {
        return new SettlementHistoryRes(
                h.getId(),
                h.getChangeType().name(),
                h.getBeforeAmount(),
                h.getAfterAmount(),
                h.getReason(),
                h.getCreatedAt()
        );
    }
}
