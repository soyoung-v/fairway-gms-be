package com.fairwaygms.fairwaygmsbe.auth.application.model.res;

import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.enums.UserStatus;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;

public record MeRes(
        Long userId,
        String email,
        String name,
        UserRole role,
        Long golfCourseId,
        UserStatus status
) {
    public static MeRes from(User user) {
        return new MeRes(user.getId(), user.getEmail(), user.getName(), user.getRole(), user.getGolfCourseId(), user.getStatus());
    }
}
