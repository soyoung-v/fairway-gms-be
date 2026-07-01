package com.fairwaygms.fairwaygmsbe.board.domain.entity;

import com.fairwaygms.fairwaygmsbe.board.domain.enums.SwapRequestStatus;
import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(name = "swap_request", indexes = {
        @Index(name = "idx_swap_request_status", columnList = "status"),
        @Index(name = "idx_swap_request_request_date", columnList = "request_date")
})
public class SwapRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "golf_course_id", nullable = false)
    private Long golfCourseId;

    @Column(name = "requester_caddie_id", nullable = false)
    private Long requesterCaddieId;

    @Column(name = "target_caddie_id")
    private Long targetCaddieId;

    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;

    @Column(name = "reason", length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SwapRequestStatus status = SwapRequestStatus.REQUESTED;

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    @Column(name = "processed_by")
    private Long processedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static SwapRequest create(Long golfCourseId, Long requesterCaddieId,
                                     Long targetCaddieId, LocalDate requestDate, String reason) {
        SwapRequest s = new SwapRequest();
        s.golfCourseId = golfCourseId;
        s.requesterCaddieId = requesterCaddieId;
        s.targetCaddieId = targetCaddieId;
        s.requestDate = requestDate;
        s.reason = reason;
        s.status = SwapRequestStatus.REQUESTED;
        s.isDeleted = false;
        return s;
    }

    public void approve(Long processedBy) {
        this.status = SwapRequestStatus.APPROVED;
        this.processedBy = processedBy;
        this.processedAt = LocalDateTime.now();
    }

    public void reject(String rejectReason, Long processedBy) {
        this.status = SwapRequestStatus.REJECTED;
        this.rejectReason = rejectReason;
        this.processedBy = processedBy;
        this.processedAt = LocalDateTime.now();
    }

    public boolean isPending() {
        return this.status == SwapRequestStatus.REQUESTED;
    }
}
