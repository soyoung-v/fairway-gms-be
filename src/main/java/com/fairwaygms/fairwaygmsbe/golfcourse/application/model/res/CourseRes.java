package com.fairwaygms.fairwaygmsbe.golfcourse.application.model.res;

import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.Course;

// 코스 응답 DTO
public record CourseRes(
        Long courseId,
        String name,
        Integer holeCount,
        Integer sortOrder,
        Boolean isActive
) {
    // 엔티티로부터 응답 DTO 생성
    public static CourseRes from(Course course) {
        return new CourseRes(
                course.getId(),
                course.getName(),
                course.getHoleCount(),
                course.getSortOrder(),
                course.getIsActive()
        );
    }
}
