package com.fairwaygms.fairwaygmsbe.caddie.domain.enums;

// 캐디 근무 패턴의 부 제한 설정. NULL이면 제한 없음
public enum PeriodLimit {
    FIRST_ONLY("1부만"),
    SECOND_ONLY("2부만");

    private final String label;

    PeriodLimit(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
