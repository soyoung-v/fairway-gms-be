package com.fairwaygms.fairwaygmsbe.operation.application.model.res;

import com.fairwaygms.fairwaygmsbe.operation.domain.entity.OperationPeriod;

import java.time.LocalTime;

public record OperationPeriodRes(
        Long periodId,
        Long courseId,
        String courseName,
        Integer periodNumber,
        LocalTime startTime,
        LocalTime endTime,
        Integer teeTimeInterval,
        Boolean isActive
) {
    public static OperationPeriodRes from(OperationPeriod period) {
        return new OperationPeriodRes(
                period.getId(),
                period.getCourse().getId(),
                period.getCourse().getName(),
                period.getPeriodNumber(),
                period.getStartTime(),
                period.getEndTime(),
                period.getTeeTimeInterval(),
                period.getIsActive()
        );
    }
}
