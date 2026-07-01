package com.fairwaygms.fairwaygmsbe.board.domain.enums;

import lombok.Getter;

@Getter
public enum SwapRequestStatus {
    REQUESTED("요청"),
    APPROVED("승인"),
    REJECTED("거절");

    private final String label;

    SwapRequestStatus(String label) {
        this.label = label;
    }
}
