package com.fairwaygms.fairwaygmsbe.golfcourse.domain.enums;

// 골프장 운영 상태
public enum GolfCourseStatus {
    OPERATING("운영중"),
    SUSPENDED("중지");

    private final String label;

    GolfCourseStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
