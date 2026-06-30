package com.fairwaygms.fairwaygmsbe.operation.domain.repository;

import com.fairwaygms.fairwaygmsbe.operation.domain.entity.OperationPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OperationPeriodRepository extends JpaRepository<OperationPeriod, Long> {

    List<OperationPeriod> findByOperationSetting_IdAndIsDeletedFalse(Long operationSettingId);

    Optional<OperationPeriod> findByIdAndIsDeletedFalse(Long id);

    // 티타임 자동생성 시 활성화된 부만 대상
    List<OperationPeriod> findByOperationSetting_IdAndIsActiveTrueAndIsDeletedFalse(Long operationSettingId);
}
