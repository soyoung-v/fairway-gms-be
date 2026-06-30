package com.fairwaygms.fairwaygmsbe.assignment.domain.repository;

import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.DailySchedule;
import com.fairwaygms.fairwaygmsbe.assignment.domain.enums.DailyScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyScheduleRepository extends JpaRepository<DailySchedule, Long> {

    // 골프장+날짜 활성 배정표 — 중복 생성 방지 및 당일 배정표 조회에 사용
    Optional<DailySchedule> findByGolfCourse_IdAndScheduleDateAndIsDeletedFalse(
            Long golfCourseId, LocalDate scheduleDate);

    // 활성 배정표 중복 여부 — 배정표 생성 전 사전 검증용
    boolean existsByGolfCourse_IdAndScheduleDateAndIsDeletedFalse(
            Long golfCourseId, LocalDate scheduleDate);

    // 확정 상태 배정표 조회 — 정산 도메인에서 월별 확정 배정 목록 조회 시 사용
    boolean existsByGolfCourse_IdAndScheduleDateAndStatusAndIsDeletedFalse(
            Long golfCourseId, LocalDate scheduleDate, DailyScheduleStatus status);
}
