package com.fairwaygms.fairwaygmsbe.settlement.application.model.res;

import com.fairwaygms.fairwaygmsbe.settlement.domain.entity.CaddieFeepolicy;

import java.math.BigDecimal;

public record FeePolicyRes(
        Long policyId,
        BigDecimal fullRoundFee,
        BigDecimal halfRoundFee,
        String halfBackType,
        String noShowPolicy,
        BigDecimal noShowFee,
        BigDecimal extraPlayerFee
) {
    public static FeePolicyRes from(CaddieFeepolicy p) {
        return new FeePolicyRes(
                p.getId(),
                p.getFullRoundFee(),
                p.getHalfRoundFee(),
                p.getHalfBackType() != null ? p.getHalfBackType().name() : null,
                p.getNoShowPolicy().name(),
                p.getNoShowFee(),
                p.getExtraPlayerFee()
        );
    }
}
