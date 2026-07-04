package com.fairwaygms.fairwaygmsbe.caddie.application.model.res;

import java.time.LocalTime;

// API-320: 캐디 모바일 내 운영 시간표 응답 (FR-327)
public record MySchedulePeriodRes(
        Integer periodNumber,
        LocalTime startTime,
        LocalTime endTime,
        String courseName
) {
}
