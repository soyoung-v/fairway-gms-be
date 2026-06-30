package com.fairwaygms.fairwaygmsbe.assignment.domain.repository;

import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.CartAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CartAssignmentRepository extends JpaRepository<CartAssignment, Long> {

    // soft-delete 구조이므로 DB UNIQUE가 운영 중 취소 건을 점유하는 문제 방지
    // 카트+날짜+티타임 중복 배정 방지는 서비스 레이어에서 검증
    boolean existsByCart_IdAndTeeTime_IdAndAssignmentDateAndIsDeletedFalse(
            Long cartId, Long teeTimeId, LocalDate assignmentDate);

    // 골프장+날짜 기준 카트 배정 목록 — 당일 카트 운용 현황 조회
    @Query("SELECT ca FROM CartAssignment ca " +
            "JOIN FETCH ca.cart " +
            "JOIN FETCH ca.teeTime " +
            "WHERE ca.golfCourse.id = :golfCourseId " +
            "AND ca.assignmentDate = :assignmentDate " +
            "AND ca.isDeleted = false " +
            "ORDER BY ca.teeTime.startTime ASC")
    List<CartAssignment> findByGolfCourseAndDate(
            @Param("golfCourseId") Long golfCourseId,
            @Param("assignmentDate") LocalDate assignmentDate);
}
