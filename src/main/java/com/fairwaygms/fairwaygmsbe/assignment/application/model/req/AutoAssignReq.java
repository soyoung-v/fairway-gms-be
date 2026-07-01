package com.fairwaygms.fairwaygmsbe.assignment.application.model.req;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

// 부(部) 단위 자동배정 요청 — 지정된 그룹의 캐디를 queueNumber 순으로 배정
// groupIds가 비어 있으면 SESSION_FIXED 제외 전체 그룹 대상
public record AutoAssignReq(
        @NotNull LocalDate assignmentDate,
        @NotNull Long periodId,
        List<Long> groupIds
) {}
