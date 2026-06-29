package com.fairwaygms.fairwaygmsbe.caddie.domain.repository;

import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieDailyStatus;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.DailyStatusType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CaddieDailyStatusRepository extends JpaRepository<CaddieDailyStatus, Long> {

    List<CaddieDailyStatus> findByGolfCourse_IdAndStatusDateAndIsDeletedFalse(Long golfCourseId, LocalDate statusDate);

    List<CaddieDailyStatus> findByCaddie_IdAndStatusDateAndIsDeletedFalse(Long caddieId, LocalDate statusDate);

    // 자동배정 제외 여부 판별용 — 해당 날짜에 특정 유형 존재 여부 확인
    boolean existsByCaddie_IdAndStatusDateAndTypeAndIsDeletedFalse(Long caddieId, LocalDate statusDate, DailyStatusType type);

    Optional<CaddieDailyStatus> findByIdAndIsDeletedFalse(Long id);
}
