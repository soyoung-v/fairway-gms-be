package com.fairwaygms.fairwaygmsbe.board.domain.enums;

import lombok.Getter;

@Getter
public enum PostCategory {
    SCHEDULE_NOTICE("시간표 공지"),
    GENERAL_NOTICE("일반 공지");

    private final String label;

    PostCategory(String label) {
        this.label = label;
    }
}
