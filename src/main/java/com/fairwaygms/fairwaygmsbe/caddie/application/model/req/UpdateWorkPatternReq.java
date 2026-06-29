package com.fairwaygms.fairwaygmsbe.caddie.application.model.req;

import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.PeriodLimit;

public record UpdateWorkPatternReq(
        Boolean canWeekday,
        Boolean canWeekend,
        PeriodLimit periodLimit,
        Boolean isFirstWaitManual
) {}
