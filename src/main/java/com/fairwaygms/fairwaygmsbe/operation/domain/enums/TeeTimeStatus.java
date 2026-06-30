package com.fairwaygms.fairwaygmsbe.operation.domain.enums;

public enum TeeTimeStatus {
    OPEN("오픈"),
    CLOSED("마감"),
    EXCLUDED("제외");

    private final String label;

    TeeTimeStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
