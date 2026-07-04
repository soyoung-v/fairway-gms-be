package com.fairwaygms.fairwaygmsbe.assignment.application.model.res;

import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.Assignment;
import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.CartAssignment;

import java.time.LocalDate;
import java.time.LocalTime;

// API-519: 캐디 모바일 내 티타임 상세 응답 (FR-527)
public record MyAssignmentDetailRes(
        Long assignmentId,
        String teamName,
        Integer playerCount,
        Long courseId,
        String courseName,
        LocalDate assignmentDate,
        LocalTime teeTime,
        String cartNumber,
        String memo,
        String playerNames,
        boolean isVip,
        String status
) {
    public static MyAssignmentDetailRes from(Assignment a, CartAssignment cartAssignment) {
        var team = a.getReservationTeam();
        var teeTimeEntity = team.getTeeTime();
        return new MyAssignmentDetailRes(
                a.getId(),
                team.getTeamName(),
                team.getPlayerCount(),
                teeTimeEntity.getCourse().getId(),
                teeTimeEntity.getCourse().getName(),
                a.getAssignmentDate(),
                teeTimeEntity.getStartTime(),
                cartAssignment != null ? cartAssignment.getCart().getCartNumber() : null,
                team.getMemo(),
                team.getPlayerNames(),
                Boolean.TRUE.equals(team.getIsVip()),
                a.getStatus().name()
        );
    }
}
