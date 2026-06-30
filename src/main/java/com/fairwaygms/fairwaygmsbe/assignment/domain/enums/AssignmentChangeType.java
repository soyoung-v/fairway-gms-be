package com.fairwaygms.fairwaygmsbe.assignment.domain.enums;

public enum AssignmentChangeType {
    AUTO("자동배정"),
    MANUAL("수동배정"),
    REASSIGN("재배정"),
    CANCEL("취소"),
    SWAP("교환"),
    CONFIRM("확정"),
    CONFIRM_CANCEL("확정취소"),
    UNLOCK("잠금해제"),
    COMPLETE("완료");

    private final String label;

    AssignmentChangeType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
