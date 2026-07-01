package com.fairwaygms.fairwaygmsbe.assignment.domain.repository;

import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.AssignmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentHistoryRepository extends JpaRepository<AssignmentHistory, Long> {

    // 단일 배정의 변경 이력 시간 순 조회 — 감사 이력 조회 API에서 사용
    List<AssignmentHistory> findByAssignment_IdOrderByCreatedAtAsc(Long assignmentId);

    // 골프장+날짜 기준 이력 조회 — 운영 이력 리포트에서 사용
    List<AssignmentHistory> findByGolfCourse_IdAndAssignment_AssignmentDateOrderByCreatedAtAsc(
            Long golfCourseId, java.time.LocalDate assignmentDate);

    // 골프장+날짜+캐디 기준 이력 조회 — 특정 캐디 필터 시 사용
    @org.springframework.data.jpa.repository.Query(
        "SELECT h FROM AssignmentHistory h " +
        "WHERE h.golfCourse.id = :golfCourseId " +
        "AND h.assignment.assignmentDate = :assignmentDate " +
        "AND (h.beforeCaddie.id = :caddieId OR h.afterCaddie.id = :caddieId) " +
        "ORDER BY h.createdAt ASC")
    List<AssignmentHistory> findByGolfCourseAndDateAndCaddie(
            @org.springframework.data.repository.query.Param("golfCourseId") Long golfCourseId,
            @org.springframework.data.repository.query.Param("assignmentDate") java.time.LocalDate assignmentDate,
            @org.springframework.data.repository.query.Param("caddieId") Long caddieId);
}
