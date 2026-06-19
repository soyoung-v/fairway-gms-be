package com.fairwaygms.fairwaygmsbe.auth.application.model.response;

public record CheckEmailResponse(boolean available) {
    public static CheckEmailResponse of(boolean available) {
        return new CheckEmailResponse(available);
    }
}
