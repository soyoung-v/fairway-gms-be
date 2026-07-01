package com.fairwaygms.fairwaygmsbe.notification.domain.enums;

import lombok.Getter;

@Getter
public enum ReferenceType {
    ASSIGNMENT("배정"),
    BOARD_POST("게시글"),
    SWAP_REQUEST("순번교환 요청");

    private final String label;

    ReferenceType(String label) {
        this.label = label;
    }
}
