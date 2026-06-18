package com.fairwaygms.fairwaygmsbe.auth.service;

import com.fairwaygms.fairwaygmsbe.auth.dto.AuthUserResponse;

public record AuthLoginResult(
        AuthUserResponse user,
        String accessToken,
        String refreshToken
) {
}
