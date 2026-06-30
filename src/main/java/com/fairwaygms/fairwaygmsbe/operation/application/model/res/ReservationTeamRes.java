package com.fairwaygms.fairwaygmsbe.operation.application.model.res;

import com.fairwaygms.fairwaygmsbe.operation.domain.entity.ReservationTeam;
import com.fairwaygms.fairwaygmsbe.operation.domain.enums.ReservationTeamStatus;

public record ReservationTeamRes(
        Long teamId,
        String teamName,
        Integer playerCount,
        ReservationTeamStatus status,
        Long designatedCaddieId,
        Boolean isVip
) {
    public static ReservationTeamRes from(ReservationTeam team) {
        return new ReservationTeamRes(
                team.getId(),
                team.getTeamName(),
                team.getPlayerCount(),
                team.getStatus(),
                team.getDesignatedCaddie() != null ? team.getDesignatedCaddie().getId() : null,
                team.getIsVip()
        );
    }
}
