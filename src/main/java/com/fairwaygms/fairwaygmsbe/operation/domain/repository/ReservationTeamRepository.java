package com.fairwaygms.fairwaygmsbe.operation.domain.repository;

import com.fairwaygms.fairwaygmsbe.operation.domain.entity.ReservationTeam;
import com.fairwaygms.fairwaygmsbe.operation.domain.enums.ReservationTeamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationTeamRepository extends JpaRepository<ReservationTeam, Long> {

    Optional<ReservationTeam> findByIdAndIsDeletedFalse(Long id);

    List<ReservationTeam> findByTeeTime_IdAndIsDeletedFalse(Long teeTimeId);

    // 날짜별 예약팀 목록 (코스/부 필터 포함 — 서비스에서 분기)
    @Query("SELECT rt FROM ReservationTeam rt JOIN rt.teeTime tt " +
           "WHERE rt.golfCourse.id = :golfCourseId AND tt.playDate = :playDate AND rt.isDeleted = false")
    List<ReservationTeam> findByGolfCourseIdAndPlayDate(@Param("golfCourseId") Long golfCourseId,
                                                        @Param("playDate") LocalDate playDate);

    long countByGolfCourse_IdAndStatusAndIsDeletedFalse(Long golfCourseId, ReservationTeamStatus status);
}
