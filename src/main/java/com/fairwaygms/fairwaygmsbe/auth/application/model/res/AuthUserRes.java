package com.fairwaygms.fairwaygmsbe.auth.application.model.res;

import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;

public record AuthUserRes(
        Long userId,
        String email,
        String name,
        UserRole role,
        Long golfCourseId
) {
    public static AuthUserRes from(User user) {
        return new AuthUserRes(user.getId(), user.getEmail(), user.getName(), user.getRole(), user.getGolfCourseId());
    }
}
