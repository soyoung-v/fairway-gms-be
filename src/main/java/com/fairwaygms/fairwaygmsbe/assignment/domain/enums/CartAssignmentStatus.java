package com.fairwaygms.fairwaygmsbe.assignment.domain.enums;

public enum CartAssignmentStatus {
    ASSIGNED("배정"),
    RETURNED("반납");

    private final String label;

    CartAssignmentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
