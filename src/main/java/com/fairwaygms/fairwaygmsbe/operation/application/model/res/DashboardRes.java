package com.fairwaygms.fairwaygmsbe.operation.application.model.res;

public record DashboardRes(
        long totalTeams,
        long availableCaddies,
        long availableCarts,
        // 배정 도메인 구현 후 연동 예정 — 현재 0 반환
        long unassignedTeams,
        long operatingCourses
) {}
