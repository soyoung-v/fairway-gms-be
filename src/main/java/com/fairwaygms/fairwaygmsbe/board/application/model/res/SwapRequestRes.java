package com.fairwaygms.fairwaygmsbe.board.application.model.res;

import com.fairwaygms.fairwaygmsbe.board.domain.entity.SwapRequest;

import java.time.LocalDate;

public record SwapRequestRes(
        Long requestId,
        String status,
        LocalDate requestDate,
        String rejectReason
) {
    public static SwapRequestRes from(SwapRequest s) {
        return new SwapRequestRes(s.getId(), s.getStatus().name(),
                s.getRequestDate(), s.getRejectReason());
    }
}
