package com.fairwaygms.fairwaygmsbe.operation.domain.repository;

import com.fairwaygms.fairwaygmsbe.operation.domain.entity.TeeTime;
import com.fairwaygms.fairwaygmsbe.operation.domain.enums.TeeTimeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface TeeTimeRepository extends JpaRepository<TeeTime, Long> {

    Optional<TeeTime> findByIdAndIsDeletedFalse(Long id);

    List<TeeTime> findByGolfCourse_IdAndPlayDateAndIsDeletedFalse(Long golfCourseId, LocalDate playDate);

    List<TeeTime> findByGolfCourse_IdAndPlayDateAndCourse_IdAndIsDeletedFalse(Long golfCourseId, LocalDate playDate, Long courseId);

    List<TeeTime> findByOperationPeriod_IdAndPlayDateAndIsDeletedFalse(Long operationPeriodId, LocalDate playDate);

    boolean existsByGolfCourse_IdAndCourse_IdAndPlayDateAndStartTimeAndIsDeletedFalse(
            Long golfCourseId, Long courseId, LocalDate playDate, LocalTime startTime);

    // 엑셀 업로드 시 코스+날짜+시간으로 단건 조회
    Optional<TeeTime> findByGolfCourse_IdAndCourse_IdAndPlayDateAndStartTimeAndIsDeletedFalse(
            Long golfCourseId, Long courseId, LocalDate playDate, LocalTime startTime);

    // 티타임 일괄 재생성 시 기간 내 전체 조회
    List<TeeTime> findByGolfCourse_IdAndPlayDateBetweenAndIsDeletedFalse(Long golfCourseId, LocalDate from, LocalDate to);

    long countByGolfCourse_IdAndPlayDateAndStatusAndIsDeletedFalse(Long golfCourseId, LocalDate playDate, TeeTimeStatus status);
}
