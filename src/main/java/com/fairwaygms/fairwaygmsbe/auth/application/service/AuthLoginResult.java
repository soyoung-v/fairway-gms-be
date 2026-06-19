package com.fairwaygms.fairwaygmsbe.auth.application.service;

import com.fairwaygms.fairwaygmsbe.auth.application.model.response.AuthUserResponse;

public record AuthLoginResult(
        AuthUserResponse user,
        String accessToken,
        String refreshToken
) {
}
