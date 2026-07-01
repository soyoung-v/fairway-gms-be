package com.fairwaygms.fairwaygmsbe.assignment.application.model.req;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

// SESSION_FIXED 그룹의 부별 일괄 수동 배정 — 시작 티타임부터 순서대로 그룹 캐디를 배정
public record BulkSessionAssignReq(
        @NotNull LocalDate assignmentDate,
        @NotNull Long startTeeTimeId,
        @NotNull Long caddieGroupId
) {}
