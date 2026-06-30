package com.fairwaygms.fairwaygmsbe.operation.domain.repository;

import com.fairwaygms.fairwaygmsbe.operation.domain.entity.SpecialOperationDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SpecialOperationDayRepository extends JpaRepository<SpecialOperationDay, Long> {

    Optional<SpecialOperationDay> findByIdAndIsDeletedFalse(Long id);

    Optional<SpecialOperationDay> findByGolfCourse_IdAndOperationDateAndIsDeletedFalse(Long golfCourseId, LocalDate operationDate);

    List<SpecialOperationDay> findByGolfCourse_IdAndOperationDateBetweenAndIsDeletedFalse(Long golfCourseId, LocalDate from, LocalDate to);

    boolean existsByGolfCourse_IdAndOperationDateAndIsDeletedFalse(Long golfCourseId, LocalDate operationDate);
}
