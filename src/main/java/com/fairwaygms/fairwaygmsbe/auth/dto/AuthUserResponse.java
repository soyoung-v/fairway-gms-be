package com.fairwaygms.fairwaygmsbe.auth.dto;

import com.fairwaygms.fairwaygmsbe.auth.domain.User;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;

public record AuthUserResponse(
        Long userId,
        String email,
        String name,
        UserRole role,
        Long golfCourseId
) {

    public static AuthUserResponse from(User user) {
        return new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getGolfCourseId()
        );
    }
}
