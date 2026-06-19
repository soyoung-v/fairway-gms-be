package com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository;

import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// 골프장 조회/저장 인터페이스
public interface GolfCourseRepository extends JpaRepository<GolfCourse, Long> {

    // 삭제되지 않은 전체 골프장 목록 조회
    List<GolfCourse> findAllByIsDeletedFalse();

    // 삭제되지 않은 골프장 단건 조회
    Optional<GolfCourse> findByIdAndIsDeletedFalse(Long id);
}
