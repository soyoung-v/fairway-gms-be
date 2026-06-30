package com.fairwaygms.fairwaygmsbe.operation.application.model.res;

import com.fairwaygms.fairwaygmsbe.operation.domain.entity.TeeTime;
import com.fairwaygms.fairwaygmsbe.operation.domain.enums.TeeTimeStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record TeeTimeRes(
        Long teeTimeId,
        LocalDate playDate,
        LocalTime startTime,
        Long courseId,
        String courseName,
        Integer periodNumber,
        TeeTimeStatus status
) {
    public static TeeTimeRes from(TeeTime teeTime) {
        return new TeeTimeRes(
                teeTime.getId(),
                teeTime.getPlayDate(),
                teeTime.getStartTime(),
                teeTime.getCourse().getId(),
                teeTime.getCourse().getName(),
                teeTime.getOperationPeriod().getPeriodNumber(),
                teeTime.getStatus()
        );
    }
}
