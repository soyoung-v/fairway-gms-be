package com.fairwaygms.fairwaygmsbe.caddie.domain.enums;

public enum CaddieStatus {
    ACTIVE("재직"),
    ON_LEAVE("휴직"),
    RESIGNED("퇴사"),
    EXCLUDED("일시제외");

    private final String label;

    CaddieStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
