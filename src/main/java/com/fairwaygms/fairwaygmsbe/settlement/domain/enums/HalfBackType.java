package com.fairwaygms.fairwaygmsbe.settlement.domain.enums;

import lombok.Getter;

@Getter
public enum HalfBackType {
    SINGLE("단일"),
    ONE_AND_HALF("1.5배"),
    DOUBLE("2배");

    private final String label;

    HalfBackType(String label) {
        this.label = label;
    }
}
