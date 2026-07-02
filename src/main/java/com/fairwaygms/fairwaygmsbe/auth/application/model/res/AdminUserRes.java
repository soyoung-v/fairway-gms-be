package com.fairwaygms.fairwaygmsbe.auth.application.model.res;

import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.enums.UserStatus;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;

import java.time.LocalDateTime;

public record AdminUserRes(
        Long userId,
        String email,
        String name,
        String phone,
        UserRole role,
        Long golfCourseId,
        UserStatus status,
        LocalDateTime createdAt,
        LocalDateTime approvedAt
) {
    public static AdminUserRes from(User user) {
        return new AdminUserRes(
                user.getId(), user.getEmail(), user.getName(), user.getPhone(),
                user.getRole(), user.getGolfCourseId(), user.getStatus(),
                user.getCreatedAt(), user.getApprovedAt()
        );
    }
}
