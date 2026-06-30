package com.fairwaygms.fairwaygmsbe.operation.domain.repository;

import com.fairwaygms.fairwaygmsbe.operation.domain.entity.OperationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OperationSettingRepository extends JpaRepository<OperationSetting, Long> {

    Optional<OperationSetting> findByGolfCourse_IdAndYearMonthAndIsDeletedFalse(Long golfCourseId, String yearMonth);

    boolean existsByGolfCourse_IdAndYearMonthAndIsDeletedFalse(Long golfCourseId, String yearMonth);

    Optional<OperationSetting> findByIdAndIsDeletedFalse(Long id);
}
