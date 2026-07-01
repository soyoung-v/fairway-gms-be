package com.fairwaygms.fairwaygmsbe.settlement.domain.enums;

import lombok.Getter;

@Getter
public enum NoShowPolicy {
    NONE("미지급"),
    HALF("반액"),
    FULL("전액");

    private final String label;

    NoShowPolicy(String label) {
        this.label = label;
    }
}
