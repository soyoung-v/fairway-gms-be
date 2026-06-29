package com.fairwaygms.fairwaygmsbe.caddie.domain.enums;

public enum DailyStatusType {
    DAY_OFF("휴무"),
    ABSENCE("결근"),
    DUTY("당번"),
    EARLY("조출"),
    SPECIAL("특수근무"),
    ASSIGN_EXCLUDED("배정제외");

    private final String label;

    DailyStatusType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
