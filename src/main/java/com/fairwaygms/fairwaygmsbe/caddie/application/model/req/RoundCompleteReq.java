package com.fairwaygms.fairwaygmsbe.caddie.application.model.req;

import java.time.LocalDateTime;

// API-314: 라운딩 완료 처리 요청 (FR-315/316) — completedAt 생략 시 현재 시각
public record RoundCompleteReq(
        LocalDateTime completedAt
) {
}
