package com.fairwaygms.fairwaygmsbe.assignment.domain.enums;

public enum DailyScheduleStatus {
    DRAFT("초안"),
    CONFIRMED("확정"),
    COMPLETED("완료");

    private final String label;

    DailyScheduleStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
