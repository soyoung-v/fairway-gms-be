package com.fairwaygms.fairwaygmsbe.auth.domain.enums;

import lombok.Getter;

@Getter
public enum OAuthProvider {

    KAKAO("카카오"),
    GOOGLE("구글"),
    NAVER("네이버");

    private final String label;

    OAuthProvider(String label) {
        this.label = label;
    }
}
