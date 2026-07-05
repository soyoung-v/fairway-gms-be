package com.fairwaygms.fairwaygmsbe.auth.application.model.req;

import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

// API-115: 소셜 최초 가입 완료 요청 (FR-115)
// name 생략 시 카카오 닉네임, email 생략 시 카카오 제공 이메일 → 없으면 대체 이메일 사용
public record OAuthCompleteReq(
        @NotNull UserRole role,
        @NotNull Long golfCourseId,
        String name,
        String phone,
        @Email String email
) {
}
