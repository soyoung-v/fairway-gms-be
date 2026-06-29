package com.fairwaygms.fairwaygmsbe.golfcourse.application.model.res;

import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;

// 골프장 응답 DTO
public record GolfCourseRes(
        Long golfCourseId,
        String name,
        String address,
        String phone,
        String status
) {
    // 엔티티로부터 응답 DTO 생성
    public static GolfCourseRes from(GolfCourse golfCourse) {
        return new GolfCourseRes(
                golfCourse.getId(),
                golfCourse.getName(),
                golfCourse.getAddress(),
                golfCourse.getPhone(),
                golfCourse.getStatus().name()
        );
    }
}
