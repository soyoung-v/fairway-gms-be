package com.fairwaygms.fairwaygmsbe.assignment.application.model.req;

import jakarta.validation.constraints.NotNull;

// 배정 교환 — 두 배정의 캐디를 서로 맞바꾼다 (post-assignment swap)
// 큐 순번 교환(SwapQueueReq)과 다름 — 이미 배정된 캐디를 맞교환하는 것
public record SwapAssignmentReq(
        @NotNull Long assignmentId1,
        @NotNull Long assignmentId2,
        String reason
) {}
