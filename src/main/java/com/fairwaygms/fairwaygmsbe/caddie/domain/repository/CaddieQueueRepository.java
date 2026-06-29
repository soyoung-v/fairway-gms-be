package com.fairwaygms.fairwaygmsbe.caddie.domain.repository;

import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieQueue;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CaddieQueueRepository extends JpaRepository<CaddieQueue, Long> {

    List<CaddieQueue> findByGolfCourse_IdAndQueueDateAndIsDeletedFalseOrderByQueueNumberAsc(Long golfCourseId, LocalDate queueDate);

    Optional<CaddieQueue> findByCaddie_IdAndQueueDateAndIsDeletedFalse(Long caddieId, LocalDate queueDate);

    // 동시 순번 조정 시 중복 방지를 위해 비관적 락 적용
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM CaddieQueue q WHERE q.golfCourse.id = :golfCourseId AND q.queueDate = :queueDate AND q.isDeleted = false ORDER BY q.queueNumber ASC")
    List<CaddieQueue> findForUpdateByGolfCourseAndDate(@Param("golfCourseId") Long golfCourseId, @Param("queueDate") LocalDate queueDate);

    boolean existsByGolfCourse_IdAndQueueDateAndQueueNumberAndIsDeletedFalse(Long golfCourseId, LocalDate queueDate, Integer queueNumber);
}
