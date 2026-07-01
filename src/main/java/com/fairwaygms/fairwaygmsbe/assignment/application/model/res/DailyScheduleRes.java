package com.fairwaygms.fairwaygmsbe.assignment.application.model.res;

import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.DailySchedule;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DailyScheduleRes(
        Long id,
        Long golfCourseId,
        LocalDate scheduleDate,
        String status,
        Long confirmedById,
        String confirmedByName,
        LocalDateTime confirmedAt
) {
    public static DailyScheduleRes from(DailySchedule d) {
        return new DailyScheduleRes(
                d.getId(),
                d.getGolfCourse().getId(),
                d.getScheduleDate(),
                d.getStatus().name(),
                d.getConfirmedBy() != null ? d.getConfirmedBy().getId() : null,
                d.getConfirmedBy() != null ? d.getConfirmedBy().getName() : null,
                d.getConfirmedAt()
        );
    }
}
