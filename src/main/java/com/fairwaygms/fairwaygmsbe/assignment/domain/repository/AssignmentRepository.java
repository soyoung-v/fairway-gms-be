package com.fairwaygms.fairwaygmsbe.assignment.domain.repository;

import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.Assignment;
import com.fairwaygms.fairwaygmsbe.assignment.domain.enums.AssignmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    // 예약팀당 활성 배정 중복 방지용 — UNIQUE 제약 대신 서비스 레이어에서 호출
    boolean existsByReservationTeam_IdAndIsDeletedFalse(Long reservationTeamId);

    // 재배정 전 기존 활성 배정 조회
    Optional<Assignment> findByReservationTeam_IdAndIsDeletedFalse(Long reservationTeamId);

    // 골프장+날짜 기준 배정 목록 — 일별 배정표 조회에 사용
    @Query("SELECT a FROM Assignment a " +
            "JOIN FETCH a.caddie " +
            "JOIN FETCH a.reservationTeam rt " +
            "JOIN FETCH rt.teeTime " +
            "WHERE a.golfCourse.id = :golfCourseId " +
            "AND a.assignmentDate = :assignmentDate " +
            "AND a.isDeleted = false " +
            "ORDER BY rt.teeTime.startTime ASC")
    List<Assignment> findByGolfCourseAndDateWithDetails(
            @Param("golfCourseId") Long golfCourseId,
            @Param("assignmentDate") LocalDate assignmentDate);

    // 캐디+날짜 기준 활성 배정 수 — 하프백 허용 여부 판단에 사용 (최대 2건)
    int countByCaddie_IdAndAssignmentDateAndIsDeletedFalse(Long caddieId, LocalDate assignmentDate);

    // 자동배정 전 캐디 배정 현황 잠금 — 중복 배정 방지를 위해 골프장+날짜 단위로 선점
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Assignment a " +
            "WHERE a.golfCourse.id = :golfCourseId " +
            "AND a.assignmentDate = :assignmentDate " +
            "AND a.isDeleted = false")
    List<Assignment> findForUpdateByGolfCourseAndDate(
            @Param("golfCourseId") Long golfCourseId,
            @Param("assignmentDate") LocalDate assignmentDate);

    // 상태별 조회 — 배정표 확정 처리 시 ASSIGNED 상태 건 일괄 CONFIRMED 변환에 사용
    @Query("SELECT a FROM Assignment a " +
            "WHERE a.golfCourse.id = :golfCourseId " +
            "AND a.assignmentDate = :assignmentDate " +
            "AND a.status = :status " +
            "AND a.isDeleted = false")
    List<Assignment> findByGolfCourseAndDateAndStatus(
            @Param("golfCourseId") Long golfCourseId,
            @Param("assignmentDate") LocalDate assignmentDate,
            @Param("status") AssignmentStatus status);
}
