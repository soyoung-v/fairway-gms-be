package com.fairwaygms.fairwaygmsbe.notification.exception;

import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCodeSpec;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum NotificationErrorCode implements ErrorCodeSpec {

    NOTIFICATION_NOT_FOUND("NOTIFICATION_NOT_FOUND", "알림을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    NOTIFICATION_ACCESS_DENIED("NOTIFICATION_ACCESS_DENIED", "해당 알림에 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    NOTIFICATION_SETTING_NOT_FOUND("NOTIFICATION_SETTING_NOT_FOUND", "알림 설정을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FCM_TOKEN_NOT_FOUND("FCM_TOKEN_NOT_FOUND", "FCM 토큰을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FCM_TOKEN_ALREADY_EXISTS("FCM_TOKEN_ALREADY_EXISTS", "이미 등록된 FCM 토큰입니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    NotificationErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
