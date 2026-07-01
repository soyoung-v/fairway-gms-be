package com.fairwaygms.fairwaygmsbe.caddie.domain.enums;

// 캐디 그룹의 자동배정 참여 방식 — ADR-005 참조
public enum CaddieGroupAssignmentType {

    // 기본 하우스 캐디 풀 — 자동배정 기본 대상, 순번 이월 방식으로 순환
    HOUSE("하우스"),

    // 특정 조건에서 배정 순서 맨 앞에 배치 (예: 주말반 — 주말 1부 첫 팀부터)
    // 자동배정 실행 시 그룹 우선순위 상단에 선택하면 활성화
    PRIORITY_FIRST("우선배정"),

    // 특정 부(部) 첫 팀부터 수동으로 먼저 배치하는 그룹 (예: 주중2부반, 3부전담반)
    // Manager가 수동 사전배정 후 나머지 팀은 하우스 자동배정으로 채움
    SESSION_FIXED("세션고정");

    private final String label;

    CaddieGroupAssignmentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
