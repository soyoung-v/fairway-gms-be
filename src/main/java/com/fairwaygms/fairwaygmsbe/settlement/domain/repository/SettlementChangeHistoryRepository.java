package com.fairwaygms.fairwaygmsbe.settlement.domain.repository;

import com.fairwaygms.fairwaygmsbe.settlement.domain.entity.SettlementChangeHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SettlementChangeHistoryRepository extends JpaRepository<SettlementChangeHistory, Long> {

    // yearMonth 기준 이력 조회. caddieId가 null이면 골프장 전체 조회
    @Query("SELECT h FROM SettlementChangeHistory h " +
           "JOIN MonthlySettlementCaddie c ON h.monthlySettlementCaddieId = c.id " +
           "JOIN MonthlySettlement s ON c.monthlySettlementId = s.id " +
           "WHERE s.golfCourseId = :golfCourseId " +
           "AND s.settlementYearMonth = :yearMonth " +
           "AND (:caddieId IS NULL OR c.caddieId = :caddieId) " +
           "ORDER BY h.createdAt DESC")
    Page<SettlementChangeHistory> findByYearMonthAndOptionalCaddie(
            @Param("golfCourseId") Long golfCourseId,
            @Param("yearMonth") String yearMonth,
            @Param("caddieId") Long caddieId,
            Pageable pageable);
}
