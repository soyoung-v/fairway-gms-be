package com.fairwaygms.fairwaygmsbe.auth.dto;

import com.fairwaygms.fairwaygmsbe.auth.domain.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.UserStatus;

public record SignupResponse(
        Long userId,
        UserStatus status
) {

    public static SignupResponse from(User user) {
        return new SignupResponse(user.getId(), user.getStatus());
    }
}
