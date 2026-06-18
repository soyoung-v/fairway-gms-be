package com.fairwaygms.fairwaygmsbe.auth.domain;

import lombok.Getter;

@Getter
public enum UserStatus {

    // 가입 승인 대기
    PENDING("승인 대기"),

    // 정상 사용 가능
    ACTIVE("활성"),

    // 보안 또는 운영 사유로 로그인 제한
    LOCKED("잠금"),

    // 사용자 탈퇴 처리
    WITHDRAWN("탈퇴"),

    // 가입 승인 거절
    REJECTED("승인 거절"),

    // 관리자 삭제 처리
    DELETED("삭제");

    // 화면 표시용 한글 상태명
    private final String label;

    // enum 문자열 저장과 화면 라벨 분리
    UserStatus(String label) {
        this.label = label;
    }
}
