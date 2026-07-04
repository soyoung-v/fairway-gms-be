package com.fairwaygms.fairwaygmsbe.caddie.application.model.res;

import java.time.LocalDateTime;

// API-314: 라운딩 완료 처리 응답 (FR-315/316)
public record RoundCompleteRes(
        Long caddieId,
        String status,
        LocalDateTime returnTime,
        boolean assignmentCompleted
) {
}
