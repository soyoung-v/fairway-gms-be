package com.fairwaygms.fairwaygmsbe.golfcourse.application.model.response;

import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.Course;

// 코스 응답 DTO
public record CourseResponse(
        Long courseId,
        String name,
        Integer holeCount,
        Integer sortOrder,
        Boolean isActive
) {
    // 엔티티로부터 응답 DTO 생성
    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getName(),
                course.getHoleCount(),
                course.getSortOrder(),
                course.getIsActive()
        );
    }
}
