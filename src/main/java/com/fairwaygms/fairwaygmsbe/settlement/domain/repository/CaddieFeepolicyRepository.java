package com.fairwaygms.fairwaygmsbe.settlement.domain.repository;

import com.fairwaygms.fairwaygmsbe.settlement.domain.entity.CaddieFeepolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CaddieFeepolicyRepository extends JpaRepository<CaddieFeepolicy, Long> {

    Optional<CaddieFeepolicy> findByGolfCourseIdAndIsDeletedFalse(Long golfCourseId);
}
