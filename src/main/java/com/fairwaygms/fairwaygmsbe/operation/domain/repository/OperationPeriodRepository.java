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

    // 수동 티타임 추가 시 setting + course + 부번호로 부 정보 조회
    Optional<OperationPeriod> findByOperationSetting_IdAndCourse_IdAndPeriodNumberAndIsDeletedFalse(
            Long operationSettingId, Long courseId, Integer periodNumber);
}
