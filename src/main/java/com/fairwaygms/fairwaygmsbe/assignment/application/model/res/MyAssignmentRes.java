package com.fairwaygms.fairwaygmsbe.assignment.application.model.res;

import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.Assignment;
import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.CartAssignment;

import java.time.LocalDate;
import java.time.LocalTime;

// API-518: 캐디 모바일 내 배정 목록 응답 (FR-526)
public record MyAssignmentRes(
        Long assignmentId,
        String teamName,
        Integer playerCount,
        Long courseId,
        String courseName,
        LocalDate assignmentDate,
        LocalTime teeTime,
        String cartNumber,
        String status,
        boolean isHalfBack
) {
    public static MyAssignmentRes from(Assignment a, CartAssignment cartAssignment) {
        var team = a.getReservationTeam();
        var teeTimeEntity = team.getTeeTime();
        return new MyAssignmentRes(
                a.getId(),
                team.getTeamName(),
                team.getPlayerCount(),
                teeTimeEntity.getCourse().getId(),
                teeTimeEntity.getCourse().getName(),
                a.getAssignmentDate(),
                teeTimeEntity.getStartTime(),
                cartAssignment != null ? cartAssignment.getCart().getCartNumber() : null,
                a.getStatus().name(),
                Boolean.TRUE.equals(a.getIsHalfBack())
        );
    }
}
