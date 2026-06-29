package com.fairwaygms.fairwaygmsbe.caddie.domain.repository;

import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieQueueHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CaddieQueueHistoryRepository extends JpaRepository<CaddieQueueHistory, Long> {

    List<CaddieQueueHistory> findByCaddie_IdAndQueueDateAndIsDeletedFalseOrderByCreatedAtDesc(Long caddieId, LocalDate queueDate);
}
