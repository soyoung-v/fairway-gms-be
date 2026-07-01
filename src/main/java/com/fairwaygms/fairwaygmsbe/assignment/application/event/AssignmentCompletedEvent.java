package com.fairwaygms.fairwaygmsbe.assignment.application.event;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

// 배정표 완료 이벤트 — 정산 도메인에서 월별 정산 집계에 사용
public class AssignmentCompletedEvent extends ApplicationEvent {

    private final Long golfCourseId;
    private final LocalDate scheduleDate;
    private final Long dailyScheduleId;

    public AssignmentCompletedEvent(Object source, Long golfCourseId, LocalDate scheduleDate,
                                    Long dailyScheduleId) {
        super(source);
        this.golfCourseId = golfCourseId;
        this.scheduleDate = scheduleDate;
        this.dailyScheduleId = dailyScheduleId;
    }

    public Long getGolfCourseId() { return golfCourseId; }
    public LocalDate getScheduleDate() { return scheduleDate; }
    public Long getDailyScheduleId() { return dailyScheduleId; }
}
