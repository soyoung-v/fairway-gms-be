package com.fairwaygms.fairwaygmsbe.settlement.domain.repository;

import com.fairwaygms.fairwaygmsbe.settlement.domain.entity.MonthlySettlementCaddie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MonthlySettlementCaddieRepository extends JpaRepository<MonthlySettlementCaddie, Long> {

    List<MonthlySettlementCaddie> findByMonthlySettlementId(Long monthlySettlementId);

    Optional<MonthlySettlementCaddie> findByMonthlySettlementIdAndCaddieId(
            Long monthlySettlementId, Long caddieId);
}
