package com.fairwaygms.fairwaygmsbe.board.domain.repository;

import com.fairwaygms.fairwaygmsbe.board.domain.entity.SwapRequest;
import com.fairwaygms.fairwaygmsbe.board.domain.enums.SwapRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SwapRequestRepository extends JpaRepository<SwapRequest, Long> {

    // Manager용: 골프장 기준 전체 목록 (status 선택 필터)
    @Query("SELECT s FROM SwapRequest s WHERE s.golfCourseId = :golfCourseId " +
           "AND s.isDeleted = false " +
           "AND (:status IS NULL OR s.status = :status) " +
           "ORDER BY s.createdAt DESC")
    Page<SwapRequest> findByGolfCourseAndStatus(@Param("golfCourseId") Long golfCourseId,
                                                @Param("status") SwapRequestStatus status,
                                                Pageable pageable);

    // Caddie용: 내 요청 목록
    Page<SwapRequest> findByRequesterCaddieIdAndIsDeletedFalseOrderByCreatedAtDesc(
            Long requesterCaddieId, Pageable pageable);
}
