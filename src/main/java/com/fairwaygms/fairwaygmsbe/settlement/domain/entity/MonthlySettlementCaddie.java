package com.fairwaygms.fairwaygmsbe.settlement.domain.entity;

import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(name = "monthly_settlement_caddie",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_monthly_settlement_caddie",
                        columnNames = {"monthly_settlement_id", "caddie_id"})
        })
public class MonthlySettlementCaddie extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "monthly_settlement_id", nullable = false)
    private Long monthlySettlementId;

    @Column(name = "caddie_id", nullable = false)
    private Long caddieId;

    @Column(name = "golf_course_id", nullable = false)
    private Long golfCourseId;

    @Column(name = "total_round_count", nullable = false)
    private Integer totalRoundCount = 0;

    @Column(name = "total_assignment_count", nullable = false)
    private Integer totalAssignmentCount = 0;

    @Column(name = "total_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalFee = BigDecimal.ZERO;

    @Column(name = "adjusted_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal adjustedFee = BigDecimal.ZERO;

    @Column(name = "adjustment_reason", length = 255)
    private String adjustmentReason;

    public static MonthlySettlementCaddie create(Long monthlySettlementId, Long caddieId,
                                                  Long golfCourseId, int totalRoundCount,
                                                  int totalAssignmentCount, BigDecimal totalFee) {
        MonthlySettlementCaddie c = new MonthlySettlementCaddie();
        c.monthlySettlementId = monthlySettlementId;
        c.caddieId = caddieId;
        c.golfCourseId = golfCourseId;
        c.totalRoundCount = totalRoundCount;
        c.totalAssignmentCount = totalAssignmentCount;
        c.totalFee = totalFee;
        c.adjustedFee = totalFee;
        return c;
    }

    public void adjustFee(BigDecimal adjustedFee, String reason) {
        this.adjustedFee = adjustedFee;
        this.adjustmentReason = reason;
    }

    public void aggregate(int additionalRounds, int additionalAssignments, BigDecimal additionalFee) {
        this.totalRoundCount += additionalRounds;
        this.totalAssignmentCount += additionalAssignments;
        this.totalFee = this.totalFee.add(additionalFee);
        this.adjustedFee = this.totalFee;
    }
}
