package com.fairwaygms.fairwaygmsbe.assignment.application.model.req;

import jakarta.validation.constraints.NotNull;

// 지정 캐디 잠금 강제 해제 (FR-512) — 사유 필수
public record UnlockAssignmentReq(
        @NotNull String reason
) {}
