package com.fairwaygms.fairwaygmsbe.settlement.domain.entity;

import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import com.fairwaygms.fairwaygmsbe.settlement.domain.enums.SettlementChangeType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(name = "settlement_change_history",
        indexes = {
                @Index(name = "idx_settlement_change_history_type", columnList = "change_type")
        })
public class SettlementChangeHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "monthly_settlement_caddie_id", nullable = false)
    private Long monthlySettlementCaddieId;

    @Column(name = "golf_course_id", nullable = false)
    private Long golfCourseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 30)
    private SettlementChangeType changeType;

    @Column(name = "before_amount", precision = 12, scale = 2)
    private BigDecimal beforeAmount;

    @Column(name = "after_amount", precision = 12, scale = 2)
    private BigDecimal afterAmount;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "changed_by")
    private Long changedBy;

    public static SettlementChangeHistory create(Long monthlySettlementCaddieId, Long golfCourseId,
                                                  SettlementChangeType changeType,
                                                  BigDecimal beforeAmount, BigDecimal afterAmount,
                                                  String reason, Long changedBy) {
        SettlementChangeHistory h = new SettlementChangeHistory();
        h.monthlySettlementCaddieId = monthlySettlementCaddieId;
        h.golfCourseId = golfCourseId;
        h.changeType = changeType;
        h.beforeAmount = beforeAmount;
        h.afterAmount = afterAmount;
        h.reason = reason;
        h.changedBy = changedBy;
        return h;
    }
}
