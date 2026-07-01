package com.fairwaygms.fairwaygmsbe.settlement.domain.repository;

import com.fairwaygms.fairwaygmsbe.settlement.domain.entity.MonthlySettlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MonthlySettlementRepository extends JpaRepository<MonthlySettlement, Long> {

    Optional<MonthlySettlement> findByGolfCourseIdAndSettlementYearMonthAndIsDeletedFalse(
            Long golfCourseId, String settlementYearMonth);
}
