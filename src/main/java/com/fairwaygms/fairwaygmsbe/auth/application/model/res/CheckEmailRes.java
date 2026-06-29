package com.fairwaygms.fairwaygmsbe.auth.application.model.res;

public record CheckEmailRes(boolean available) {
    public static CheckEmailRes of(boolean available) {
        return new CheckEmailRes(available);
    }
}
