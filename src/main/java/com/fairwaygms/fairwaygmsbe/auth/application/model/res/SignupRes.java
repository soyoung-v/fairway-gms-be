package com.fairwaygms.fairwaygmsbe.auth.application.model.res;

import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.enums.UserStatus;

public record SignupRes(
        Long userId,
        UserStatus status
) {
    public static SignupRes from(User user) {
        return new SignupRes(user.getId(), user.getStatus());
    }
}
