package com.fairwaygms.fairwaygmsbe.assignment.application.model.res;

import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.Assignment;

import java.time.LocalDate;

public record AssignmentRes(
        Long id,
        Long reservationTeamId,
        Long teeTimeId,
        Long caddieId,
        String caddieName,
        LocalDate assignmentDate,
        String status,
        boolean isLocked,
        boolean isHalfBack
) {
    public static AssignmentRes from(Assignment a) {
        return new AssignmentRes(
                a.getId(),
                a.getReservationTeam().getId(),
                a.getReservationTeam().getTeeTime().getId(),
                a.getCaddie().getId(),
                a.getCaddie().getName(),
                a.getAssignmentDate(),
                a.getStatus().name(),
                a.getIsLocked(),
                a.getIsHalfBack()
        );
    }
}
