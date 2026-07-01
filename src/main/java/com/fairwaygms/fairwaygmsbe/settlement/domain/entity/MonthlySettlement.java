package com.fairwaygms.fairwaygmsbe.settlement.domain.entity;

import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import com.fairwaygms.fairwaygmsbe.settlement.domain.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(name = "monthly_settlement",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_monthly_settlement_golf_year_month",
                        columnNames = {"golf_course_id", "settlement_year_month"})
        })
public class MonthlySettlement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "golf_course_id", nullable = false)
    private Long golfCourseId;

    @Column(name = "settlement_year_month", nullable = false, length = 7)
    private String settlementYearMonth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SettlementStatus status = SettlementStatus.AGGREGATING;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static MonthlySettlement create(Long golfCourseId, String settlementYearMonth) {
        MonthlySettlement s = new MonthlySettlement();
        s.golfCourseId = golfCourseId;
        s.settlementYearMonth = settlementYearMonth;
        s.status = SettlementStatus.AGGREGATING;
        s.isDeleted = false;
        return s;
    }

    public void confirm() {
        this.status = SettlementStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }

    public void cancelConfirm() {
        this.status = SettlementStatus.AGGREGATING;
        this.confirmedAt = null;
    }

    public boolean isConfirmed() {
        return this.status == SettlementStatus.CONFIRMED;
    }
}
