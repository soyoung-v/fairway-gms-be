package com.fairwaygms.fairwaygmsbe.golfcourse.application.model.res;

import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;

// 비로그인 공개 골프장 목록 응답 — 회원가입 골프장 선택 드롭다운용 (최소 정보만 노출)
public record PublicGolfCourseRes(
        Long golfCourseId,
        String name
) {
    public static PublicGolfCourseRes from(GolfCourse golfCourse) {
        return new PublicGolfCourseRes(golfCourse.getId(), golfCourse.getName());
    }
}
