package com.fairwaygms.fairwaygmsbe.golfcourse.domain.enums;

// 카트 종류: 전동카트 또는 일반(수동) 카트
public enum CartType {
    ELECTRIC("전동"),
    MANUAL("일반");

    private final String label;

    CartType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
