package com.fairwaygms.fairwaygmsbe.assignment.application.model.res;

// API-509: 배정 검증 오류 응답 (FR-515/516) — type: DUPLICATE / OFF_DUTY
public record ValidationErrorRes(
        String type,
        Long caddieId,
        String caddieName,
        String message
) {
}
