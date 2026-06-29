package com.fairwaygms.fairwaygmsbe.caddie.domain.repository;

import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieWorkPattern;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CaddieWorkPatternRepository extends JpaRepository<CaddieWorkPattern, Long> {

    Optional<CaddieWorkPattern> findByCaddie_IdAndIsDeletedFalse(Long caddieId);
}
