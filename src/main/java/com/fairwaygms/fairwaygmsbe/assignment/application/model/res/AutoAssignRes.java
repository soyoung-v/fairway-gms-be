package com.fairwaygms.fairwaygmsbe.assignment.application.model.res;

import java.time.LocalDate;

// 부별 자동배정 결과 요약
public record AutoAssignRes(
        LocalDate assignmentDate,
        Long periodId,
        int assignedCount,
        int skippedCount
) {}
