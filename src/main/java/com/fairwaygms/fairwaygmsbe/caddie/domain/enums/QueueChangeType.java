package com.fairwaygms.fairwaygmsbe.caddie.domain.enums;

public enum QueueChangeType {
    RESET("초기화"),
    MANUAL_ADJUST("수동조정"),
    SWAP("교환");

    private final String label;

    QueueChangeType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
