package com.fairwaygms.fairwaygmsbe.auth.dto;

// 이메일 중복 확인 응답. available=true이면 가입 가능한 이메일이다.
public record CheckEmailResponse(boolean available) {

    public static CheckEmailResponse of(boolean available) {
        return new CheckEmailResponse(available);
    }
}
