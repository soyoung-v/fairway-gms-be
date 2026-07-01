package com.fairwaygms.fairwaygmsbe.settlement.domain.repository;

import com.fairwaygms.fairwaygmsbe.settlement.domain.entity.AssignmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssignmentRecordRepository extends JpaRepository<AssignmentRecord, Long> {

    Optional<AssignmentRecord> findByAssignmentIdAndIsDeletedFalse(Long assignmentId);

    List<AssignmentRecord> findByGolfCourseIdAndSettlementYearMonthAndIsDeletedFalse(
            Long golfCourseId, String settlementYearMonth);

    // 캐디별 라운드/배정/캐디피 집계 — 정산 집계 화면용
    @Query("SELECT r.caddieId, COUNT(DISTINCT r.playDate), COUNT(r.id), COALESCE(SUM(r.feeAmount), 0) " +
           "FROM AssignmentRecord r " +
           "WHERE r.golfCourseId = :golfCourseId AND r.settlementYearMonth = :yearMonth " +
           "AND r.isDeleted = false " +
           "GROUP BY r.caddieId")
    List<Object[]> aggregateByCaddie(@Param("golfCourseId") Long golfCourseId,
                                     @Param("yearMonth") String yearMonth);
}
