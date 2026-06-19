package com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository;

import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.Course;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// 코스 조회/저장 인터페이스
public interface CourseRepository extends JpaRepository<Course, Long> {

    // 특정 골프장의 삭제되지 않은 코스 목록 조회
    List<Course> findAllByGolfCourseAndIsDeletedFalse(GolfCourse golfCourse);

    // 삭제되지 않은 코스 단건 조회
    Optional<Course> findByIdAndIsDeletedFalse(Long id);

    // 같은 골프장 내 동일 코스명 중복 확인
    boolean existsByGolfCourseAndNameAndIsDeletedFalse(GolfCourse golfCourse, String name);
}
