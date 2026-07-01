package com.fairwaygms.fairwaygmsbe.settlement.domain.enums;

import lombok.Getter;

@Getter
public enum SettlementStatus {
    AGGREGATING("집계중"),
    CONFIRMED("확정");

    private final String label;

    SettlementStatus(String label) {
        this.label = label;
    }
}
