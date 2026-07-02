package com.fairwaygms.fairwaygmsbe.board.application.model.res;

import com.fairwaygms.fairwaygmsbe.board.domain.entity.SwapRequest;

import java.time.LocalDate;

public record SwapRequestSummaryRes(
        Long requestId,
        String requesterName,
        String targetName,
        LocalDate requestDate,
        String status,
        String rejectReason
) {
    public static SwapRequestSummaryRes of(SwapRequest s, String requesterName, String targetName) {
        return new SwapRequestSummaryRes(s.getId(), requesterName, targetName,
                s.getRequestDate(), s.getStatus().name(), s.getRejectReason());
    }
}
