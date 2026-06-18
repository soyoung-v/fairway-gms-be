package com.fairwaygms.fairwaygmsbe.auth.domain;

import lombok.Getter;

@Getter
public enum OAuthProvider {

    // 카카오 소셜 로그인 제공자
    KAKAO("카카오"),

    // 구글 소셜 로그인 제공자
    GOOGLE("구글"),

    // 네이버 소셜 로그인 제공자
    NAVER("네이버");

    // 화면 표시용 한글 제공자명
    private final String label;

    // enum 문자열 저장과 화면 라벨 분리
    OAuthProvider(String label) {
        this.label = label;
    }
}
