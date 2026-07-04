package com.fairwaygms.fairwaygmsbe.assignment.application.model.res;

// API-502: 하프백(투근무) 배정 응답 (FR-506)
public record HalfBackAssignRes(
        Long assignmentId1,
        Long assignmentId2,
        boolean isHalfBack
) {
}
