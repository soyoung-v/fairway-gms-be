package com.fairwaygms.fairwaygmsbe.auth.application.model.response;

import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.enums.UserStatus;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long userId,
        String email,
        String name,
        String phone,
        UserRole role,
        Long golfCourseId,
        UserStatus status,
        LocalDateTime approvedAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(), user.getEmail(), user.getName(), user.getPhone(),
                user.getRole(), user.getGolfCourseId(), user.getStatus(), user.getApprovedAt()
        );
    }
}
