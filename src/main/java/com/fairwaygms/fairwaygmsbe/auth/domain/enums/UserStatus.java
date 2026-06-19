package com.fairwaygms.fairwaygmsbe.auth.domain.enums;

import lombok.Getter;

@Getter
public enum UserStatus {

    PENDING("승인 대기"),
    ACTIVE("활성"),
    LOCKED("잠금"),
    WITHDRAWN("탈퇴"),
    REJECTED("승인 거절"),
    DELETED("삭제");

    private final String label;

    UserStatus(String label) {
        this.label = label;
    }
}
