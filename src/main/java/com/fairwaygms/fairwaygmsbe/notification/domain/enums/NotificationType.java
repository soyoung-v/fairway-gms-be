package com.fairwaygms.fairwaygmsbe.notification.domain.enums;

import lombok.Getter;

@Getter
public enum NotificationType {
    ASSIGNMENT_CONFIRMED("배정표 확정"),
    ASSIGNMENT_CHANGED("배정 변경"),
    BOARD_POST_CREATED("게시글 등록"),
    SWAP_RESULT("순번교환 결과");

    private final String label;

    NotificationType(String label) {
        this.label = label;
    }
}
