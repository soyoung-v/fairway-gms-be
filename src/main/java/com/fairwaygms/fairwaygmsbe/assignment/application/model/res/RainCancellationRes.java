package com.fairwaygms.fairwaygmsbe.assignment.application.model.res;

// API-508: 우천취소 반영 응답 (FR-513/514)
public record RainCancellationRes(
        String message,
        boolean queueHandled
) {
}
