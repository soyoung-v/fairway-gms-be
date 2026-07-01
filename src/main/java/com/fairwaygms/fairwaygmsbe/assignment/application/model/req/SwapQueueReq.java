package com.fairwaygms.fairwaygmsbe.assignment.application.model.req;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

// 당일 큐 순번 교환 — 두 캐디의 queueNumber를 교환한다
// rotation cursor에는 영향 없음 (ADR-005 Decision 6)
public record SwapQueueReq(
        @NotNull Long caddieAId,
        @NotNull Long caddieBId,
        @NotNull LocalDate queueDate
) {}
