package com.fairwaygms.fairwaygmsbe.auth.application.service;

import com.fairwaygms.fairwaygmsbe.auth.application.model.res.AuthUserRes;

public record AuthLoginResult(
        AuthUserRes user,
        String accessToken,
        String refreshToken
) {
}
