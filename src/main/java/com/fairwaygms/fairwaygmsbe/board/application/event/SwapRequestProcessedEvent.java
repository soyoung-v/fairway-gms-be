package com.fairwaygms.fairwaygmsbe.board.application.event;

import com.fairwaygms.fairwaygmsbe.board.domain.enums.SwapRequestStatus;
import org.springframework.context.ApplicationEvent;

// 순번교환 요청 처리(승인/거절) 이벤트 — 요청자 캐디에게 FCM 알림 발송에 사용
public class SwapRequestProcessedEvent extends ApplicationEvent {

    private final Long swapRequestId;
    private final Long requesterCaddieId;
    private final SwapRequestStatus status;
    private final String rejectReason;

    public SwapRequestProcessedEvent(Object source, Long swapRequestId, Long requesterCaddieId,
                                     SwapRequestStatus status, String rejectReason) {
        super(source);
        this.swapRequestId = swapRequestId;
        this.requesterCaddieId = requesterCaddieId;
        this.status = status;
        this.rejectReason = rejectReason;
    }

    public Long getSwapRequestId() { return swapRequestId; }
    public Long getRequesterCaddieId() { return requesterCaddieId; }
    public SwapRequestStatus getStatus() { return status; }
    public String getRejectReason() { return rejectReason; }
}
