package com.fairwaygms.fairwaygmsbe.operation.domain.enums;

public enum RainCancellationPolicyType {
    KEEP_ORDER("순번유지"),
    RESEQUENCE("재정렬");

    private final String label;

    RainCancellationPolicyType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
