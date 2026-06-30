package com.fairwaygms.fairwaygmsbe.assignment.domain.enums;

public enum AssignmentStatus {
    ASSIGNED("배정"),
    CONFIRMED("확정"),
    CANCELLED("취소"),
    COMPLETED("완료");

    private final String label;

    AssignmentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
