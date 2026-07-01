package com.fairwaygms.fairwaygmsbe.settlement.domain.enums;

import lombok.Getter;

@Getter
public enum SettlementChangeType {
    FEE_EDIT("캐디피 수정"),
    RAIN_CANCEL("우천취소 반영"),
    MANUAL_ADJUST("수동 조정"),
    CONFIRM("월 마감 확정"),
    CONFIRM_CANCEL("확정 취소");

    private final String label;

    SettlementChangeType(String label) {
        this.label = label;
    }
}
