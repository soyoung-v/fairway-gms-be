package com.fairwaygms.fairwaygmsbe.assignment.application.model.res;

import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.CartAssignment;

import java.time.LocalDate;
import java.time.LocalTime;

public record CartAssignmentRes(
        Long id,
        Long cartId,
        String cartNumber,
        Long teeTimeId,
        LocalTime teeTimeStartTime,
        LocalDate assignmentDate,
        String status
) {
    public static CartAssignmentRes from(CartAssignment c) {
        return new CartAssignmentRes(
                c.getId(),
                c.getCart().getId(),
                c.getCart().getCartNumber(),
                c.getTeeTime().getId(),
                c.getTeeTime().getStartTime(),
                c.getAssignmentDate(),
                c.getStatus().name()
        );
    }
}
