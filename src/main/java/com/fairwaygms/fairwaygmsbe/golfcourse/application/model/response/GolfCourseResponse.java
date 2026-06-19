package com.fairwaygms.fairwaygmsbe.golfcourse.application.model.response;

import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;

// 골프장 응답 DTO
public record GolfCourseResponse(
        Long golfCourseId,
        String name,
        String address,
        String phone,
        String status
) {
    // 엔티티로부터 응답 DTO 생성
    public static GolfCourseResponse from(GolfCourse golfCourse) {
        return new GolfCourseResponse(
                golfCourse.getId(),
                golfCourse.getName(),
                golfCourse.getAddress(),
                golfCourse.getPhone(),
                golfCourse.getStatus().name()
        );
    }
}
