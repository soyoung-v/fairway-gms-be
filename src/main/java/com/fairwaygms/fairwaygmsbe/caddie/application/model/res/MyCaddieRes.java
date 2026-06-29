package com.fairwaygms.fairwaygmsbe.caddie.application.model.res;

import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieWorkPattern;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.CaddieStatus;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.PeriodLimit;

public record MyCaddieRes(
        Long caddieId,
        String name,
        String caddieNumber,
        CaddieStatus status,
        String statusLabel,
        WorkPatternSummary workPattern
) {
    public record WorkPatternSummary(
            Boolean canWeekday,
            Boolean canWeekend,
            PeriodLimit periodLimit,
            Boolean isFirstWaitManual
    ) {
        public static WorkPatternSummary from(CaddieWorkPattern pattern) {
            return new WorkPatternSummary(
                    pattern.getCanWeekday(),
                    pattern.getCanWeekend(),
                    pattern.getPeriodLimit(),
                    pattern.getIsFirstWaitManual()
            );
        }
    }

    public static MyCaddieRes of(Caddie caddie, CaddieWorkPattern pattern) {
        return new MyCaddieRes(
                caddie.getId(),
                caddie.getName(),
                caddie.getCaddieNumber(),
                caddie.getStatus(),
                caddie.getStatus().getLabel(),
                pattern != null ? WorkPatternSummary.from(pattern) : null
        );
    }
}
