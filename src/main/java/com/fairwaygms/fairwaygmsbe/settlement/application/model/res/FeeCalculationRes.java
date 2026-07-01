package com.fairwaygms.fairwaygmsbe.settlement.application.model.res;

import java.math.BigDecimal;

public record FeeCalculationRes(
        BigDecimal calculatedFee,
        String completionType,
        String noShowPolicy,
        Integer playedHoleCount
) {
    public static FeeCalculationRes rainCancellation(BigDecimal fee, int playedHoleCount) {
        return new FeeCalculationRes(fee, "RAIN_CANCELLED", null, playedHoleCount);
    }

    public static FeeCalculationRes noShow(BigDecimal fee, String noShowPolicy) {
        return new FeeCalculationRes(fee, "NO_SHOW", noShowPolicy, null);
    }
}
