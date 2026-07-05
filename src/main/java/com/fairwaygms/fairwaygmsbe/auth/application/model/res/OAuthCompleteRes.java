package com.fairwaygms.fairwaygmsbe.auth.application.model.res;

// API-115: 소셜 최초 가입 완료 응답 — status=PENDING (관리자/매니저 승인 대기)
public record OAuthCompleteRes(
        Long userId,
        String status
) {
}
