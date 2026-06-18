package com.fairwaygms.fairwaygmsbe.common.security;

import lombok.Getter;

// JWT 인증이 완료된 후 SecurityContext에 저장되는 로그인 사용자 정보.
// Controller나 Service에서 현재 로그인한 사용자가 누구인지 확인할 때 이 객체를 꺼내 쓴다.
// 예: 어느 골프장 소속인지, 어떤 역할인지 판단하는 기준이 된다.
@Getter
public class AuthenticatedUser {

    // 로그인한 사용자의 고유 ID (app_user 테이블의 PK)
    private final Long userId;

    // 사용자 역할 (ADMIN / MANAGER / CADDY)
    private final UserRole role;

    // 소속 골프장 ID. MANAGER와 CADDY는 값이 있고, ADMIN은 null이다.
    // ADMIN은 X-Selected-Golf-Course-Id 헤더로 대상 골프장을 선택한다.
    private final Long golfCourseId;

    public AuthenticatedUser(Long userId, UserRole role, Long golfCourseId) {
        this.userId = userId;
        this.role = role;
        this.golfCourseId = golfCourseId;
    }

    // 플랫폼 최고 관리자인지 확인
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    // 골프장 매니저인지 확인
    public boolean isManager() {
        return role == UserRole.MANAGER;
    }

    // 캐디인지 확인
    public boolean isCaddy() {
        return role == UserRole.CADDY;
    }
}
