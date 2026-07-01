package com.fairwaygms.fairwaygmsbe.settlement.domain.enums;

import lombok.Getter;

@Getter
public enum CompletionType {
    NORMAL("정상 완료"),
    RAIN_CANCELLED("우천취소"),
    MID_CANCELLED("중간 취소"),
    NO_SHOW("노쇼");

    private final String label;

    CompletionType(String label) {
        this.label = label;
    }
}
