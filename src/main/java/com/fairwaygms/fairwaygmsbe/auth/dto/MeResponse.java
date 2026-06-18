package com.fairwaygms.fairwaygmsbe.auth.dto;

import com.fairwaygms.fairwaygmsbe.auth.domain.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.UserStatus;
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
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getGolfCourseId(),
                user.getStatus()
        );
    }
}
