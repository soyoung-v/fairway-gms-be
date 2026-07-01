package com.fairwaygms.fairwaygmsbe.assignment.application.model.res;

import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.AssignmentHistory;

import java.time.LocalDateTime;

public record AssignmentHistoryRes(
        Long id,
        Long assignmentId,
        String changeType,
        Long beforeCaddieId,
        String beforeCaddieName,
        Long afterCaddieId,
        String afterCaddieName,
        String reason,
        String changedByName,
        LocalDateTime createdAt
) {
    public static AssignmentHistoryRes from(AssignmentHistory h) {
        return new AssignmentHistoryRes(
                h.getId(),
                h.getAssignment().getId(),
                h.getChangeType().name(),
                h.getBeforeCaddie() != null ? h.getBeforeCaddie().getId() : null,
                h.getBeforeCaddie() != null ? h.getBeforeCaddie().getName() : null,
                h.getAfterCaddie() != null ? h.getAfterCaddie().getId() : null,
                h.getAfterCaddie() != null ? h.getAfterCaddie().getName() : null,
                h.getReason(),
                h.getChangedBy().getName(),
                h.getCreatedAt()
        );
    }
}
