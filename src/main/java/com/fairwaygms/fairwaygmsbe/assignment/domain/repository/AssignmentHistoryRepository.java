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
}
