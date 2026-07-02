package com.fairwaygms.fairwaygmsbe.assignment.application.model.res;

import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.Assignment;
import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.CartAssignment;

import java.time.LocalTime;

public record CourseAssignmentRes(
        Long assignmentId,
        Long teamId,
        String teamName,
        LocalTime teeTime,
        Integer periodNumber,
        Long caddieId,
        String caddieName,
        Long cartId,
        String cartNumber,
        String status,
        boolean isLocked
) {
    public static CourseAssignmentRes from(Assignment a, CartAssignment cartAssignment) {
        var teeTimeEntity = a.getReservationTeam().getTeeTime();
        return new CourseAssignmentRes(
                a.getId(),
                a.getReservationTeam().getId(),
                a.getReservationTeam().getTeamName(),
                teeTimeEntity.getStartTime(),
                teeTimeEntity.getOperationPeriod().getPeriodNumber(),
                a.getCaddie().getId(),
                a.getCaddie().getName(),
                cartAssignment != null ? cartAssignment.getCart().getId() : null,
                cartAssignment != null ? cartAssignment.getCart().getCartNumber() : null,
                a.getStatus().name(),
                a.getIsLocked()
        );
    }
}
