package com.fairwaygms.fairwaygmsbe.auth.application.model.response;

import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.enums.UserStatus;

public record SignupResponse(
        Long userId,
        UserStatus status
) {
    public static SignupResponse from(User user) {
        return new SignupResponse(user.getId(), user.getStatus());
    }
}
