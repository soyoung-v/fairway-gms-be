package com.fairwaygms.fairwaygmsbe.assignment.application.model.res;

import com.fairwaygms.fairwaygmsbe.operation.domain.entity.ReservationTeam;

import java.time.LocalTime;

public record UnassignedTeamRes(
        Long teamId,
        String teamName,
        Long teeTimeId,
        LocalTime teeTime,
        Long courseId,
        String courseName,
        Integer periodNumber
) {
    public static UnassignedTeamRes from(ReservationTeam t) {
        return new UnassignedTeamRes(
                t.getId(),
                t.getTeamName(),
                t.getTeeTime().getId(),
                t.getTeeTime().getStartTime(),
                t.getTeeTime().getCourse().getId(),
                t.getTeeTime().getCourse().getName(),
                t.getTeeTime().getOperationPeriod().getPeriodNumber()
        );
    }
}
