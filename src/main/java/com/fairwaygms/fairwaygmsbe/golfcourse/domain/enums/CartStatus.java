package com.fairwaygms.fairwaygmsbe.golfcourse.domain.enums;

// 카트 현재 상태: 사용가능 / 점검중 / 사용중지
public enum CartStatus {
    AVAILABLE("사용가능"),
    MAINTENANCE("점검"),
    DISABLED("사용중지");

    private final String label;

    CartStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
