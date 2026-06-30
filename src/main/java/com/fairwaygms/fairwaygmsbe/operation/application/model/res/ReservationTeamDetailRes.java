package com.fairwaygms.fairwaygmsbe.operation.application.model.res;

import com.fairwaygms.fairwaygmsbe.operation.domain.entity.ReservationTeam;
import com.fairwaygms.fairwaygmsbe.operation.domain.enums.ReservationTeamStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReservationTeamDetailRes(
        Long teamId,
        Long teeTimeId,
        LocalDate playDate,
        LocalTime teeTime,
        Long courseId,
        String courseName,
        Integer playerCount,
        ReservationTeamStatus status,
        Long designatedCaddieId,
        Boolean isVip,
        String playerNames,
        String memo
) {
    public static ReservationTeamDetailRes from(ReservationTeam team) {
        return new ReservationTeamDetailRes(
                team.getId(),
                team.getTeeTime().getId(),
                team.getTeeTime().getPlayDate(),
                team.getTeeTime().getStartTime(),
                team.getTeeTime().getCourse().getId(),
                team.getTeeTime().getCourse().getName(),
                team.getPlayerCount(),
                team.getStatus(),
                team.getDesignatedCaddie() != null ? team.getDesignatedCaddie().getId() : null,
                team.getIsVip(),
                team.getPlayerNames(),
                team.getMemo()
        );
    }
}
