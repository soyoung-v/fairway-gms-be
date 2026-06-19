package com.fairwaygms.fairwaygmsbe.auth.application.model.response;

import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.enums.UserStatus;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;

public record MeResponse(
        Long userId,
        String email,
        String name,
        UserRole role,
        Long golfCourseId,
        UserStatus status
) {
    public static MeResponse from(User user) {
        return new MeResponse(user.getId(), user.getEmail(), user.getName(), user.getRole(), user.getGolfCourseId(), user.getStatus());
    }
}
