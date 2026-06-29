package com.fairwaygms.fairwaygmsbe.caddie.domain.enums;

// 당번 유형(DUTY) 전용 우선순위 구분
public enum DailyStatusPriority {
    FIRST("1당번"),
    SECOND("2당번");

    private final String label;

    DailyStatusPriority(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
