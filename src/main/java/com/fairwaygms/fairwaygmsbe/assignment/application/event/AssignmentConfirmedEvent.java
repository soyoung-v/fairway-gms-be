package com.fairwaygms.fairwaygmsbe.assignment.application.event;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

// 배정표 확정 이벤트 — AFTER_COMMIT 리스너에서 알림·게시판 시간표 생성에 사용
public class AssignmentConfirmedEvent extends ApplicationEvent {

    private final Long golfCourseId;
    private final LocalDate scheduleDate;
    private final Long dailyScheduleId;
    private final Long confirmedByUserId;

    public AssignmentConfirmedEvent(Object source, Long golfCourseId, LocalDate scheduleDate,
                                    Long dailyScheduleId, Long confirmedByUserId) {
        super(source);
        this.golfCourseId = golfCourseId;
        this.scheduleDate = scheduleDate;
        this.dailyScheduleId = dailyScheduleId;
        this.confirmedByUserId = confirmedByUserId;
    }

    public Long getGolfCourseId() { return golfCourseId; }
    public LocalDate getScheduleDate() { return scheduleDate; }
    public Long getDailyScheduleId() { return dailyScheduleId; }
    public Long getConfirmedByUserId() { return confirmedByUserId; }
}
