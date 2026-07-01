package com.fairwaygms.fairwaygmsbe.settlement.domain.entity;

import com.fairwaygms.fairwaygmsbe.settlement.domain.enums.CompletionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "assignment_record",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_assignment_record_assignment", columnNames = "assignment_id")
        },
        indexes = {
                @Index(name = "idx_assignment_record_golf_year_month",
                        columnList = "golf_course_id, settlement_year_month")
        })
public class AssignmentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "golf_course_id", nullable = false)
    private Long golfCourseId;

    @Column(name = "assignment_id", nullable = false, unique = true)
    private Long assignmentId;

    @Column(name = "caddie_id", nullable = false)
    private Long caddieId;

    @Column(name = "play_date", nullable = false)
    private LocalDate playDate;

    @Column(name = "settlement_year_month", nullable = false, length = 7)
    private String settlementYearMonth;

    @Column(name = "played_hole_count")
    private Integer playedHoleCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_type", nullable = false, length = 30)
    private CompletionType completionType;

    @Column(name = "fee_amount", precision = 10, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "is_confirmed", nullable = false)
    private Boolean isConfirmed = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static AssignmentRecord create(Long golfCourseId, Long assignmentId, Long caddieId,
                                          LocalDate playDate, String settlementYearMonth,
                                          CompletionType completionType, Integer playedHoleCount,
                                          BigDecimal feeAmount) {
        AssignmentRecord r = new AssignmentRecord();
        r.golfCourseId = golfCourseId;
        r.assignmentId = assignmentId;
        r.caddieId = caddieId;
        r.playDate = playDate;
        r.settlementYearMonth = settlementYearMonth;
        r.completionType = completionType;
        r.playedHoleCount = playedHoleCount;
        r.feeAmount = feeAmount;
        r.isConfirmed = false;
        r.isDeleted = false;
        return r;
    }

    public void confirm() {
        this.isConfirmed = true;
    }

    public void updateFee(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }
}
