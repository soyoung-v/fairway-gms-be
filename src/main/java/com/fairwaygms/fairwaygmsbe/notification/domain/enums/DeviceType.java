package com.fairwaygms.fairwaygmsbe.notification.domain.enums;

import lombok.Getter;

@Getter
public enum DeviceType {
    WEB("웹"),
    MOBILE("모바일");

    private final String label;

    DeviceType(String label) {
        this.label = label;
    }
}
