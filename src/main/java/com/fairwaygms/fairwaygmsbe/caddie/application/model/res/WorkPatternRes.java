package com.fairwaygms.fairwaygmsbe.caddie.application.model.res;

import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieWorkPattern;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.PeriodLimit;

public record WorkPatternRes(
        Long caddieId,
        Boolean canWeekday,
        Boolean canWeekend,
        PeriodLimit periodLimit,
        Boolean isFirstWaitManual
) {
    public static WorkPatternRes from(CaddieWorkPattern pattern) {
        return new WorkPatternRes(
                pattern.getCaddie().getId(),
                pattern.getCanWeekday(),
                pattern.getCanWeekend(),
                pattern.getPeriodLimit(),
                pattern.getIsFirstWaitManual()
        );
    }
}
