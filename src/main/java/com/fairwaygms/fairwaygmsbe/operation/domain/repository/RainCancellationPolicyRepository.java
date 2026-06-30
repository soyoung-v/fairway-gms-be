package com.fairwaygms.fairwaygmsbe.operation.domain.repository;

import com.fairwaygms.fairwaygmsbe.operation.domain.entity.RainCancellationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RainCancellationPolicyRepository extends JpaRepository<RainCancellationPolicy, Long> {

    // 골프장당 1건
    Optional<RainCancellationPolicy> findByGolfCourse_IdAndIsDeletedFalse(Long golfCourseId);
}
