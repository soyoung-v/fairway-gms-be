package com.fairwaygms.fairwaygmsbe.common.security;

import lombok.Getter;

// 시스템에서 사용하는 사용자 역할 목록.
// DB에는 영문 이름 그대로 저장되고 (예: "ADMIN"), 화면에는 label을 사용한다.
// common 패키지에 위치하는 이유: GolfCourseContextResolver 등 공통 기반에서 역할 분기가 필요하기 때문.
// auth 패키지에 두면 common ↔ auth 간 순환 의존이 생기므로 common에 배치한다.
@Getter
public enum UserRole {

    // 플랫폼 전체를 관리하는 최고 관리자. 모든 골프장 데이터에 접근 가능.
    ADMIN("관리자"),

    // 특정 골프장을 운영하는 매니저. 자신이 소속된 골프장 데이터만 접근 가능.
    MANAGER("매니저"),

    // 골프장 캐디. PWA 모바일 앱에서 자신의 배정 정보를 확인한다.
    CADDY("캐디");

    // 화면에 표시할 한글 이름
    private final String label;

    UserRole(String label) {
        this.label = label;
    }
}
