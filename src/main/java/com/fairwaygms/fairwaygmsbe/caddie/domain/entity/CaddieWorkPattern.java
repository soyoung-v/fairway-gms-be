package com.fairwaygms.fairwaygmsbe.caddie.domain.entity;

import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.PeriodLimit;
import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 캐디당 1건만 존재. 자동배정 시 주중/주말 여부, 부 제한 조건으로 사용
@Getter
@Entity
@Table(
        name = "caddie_work_pattern",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_caddie_work_pattern_caddie", columnNames = {"caddie_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaddieWorkPattern extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caddie_id", nullable = false)
    private Caddie caddie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "golf_course_id", nullable = false)
    private GolfCourse golfCourse;

    @Column(name = "can_weekday", nullable = false)
    private Boolean canWeekday = true;

    @Column(name = "can_weekend", nullable = false)
    private Boolean canWeekend = true;

    // NULL이면 부 제한 없음
    @Enumerated(EnumType.STRING)
    @Column(name = "period_limit", length = 30)
    private PeriodLimit periodLimit;

    @Column(name = "is_first_wait_manual", nullable = false)
    private Boolean isFirstWaitManual = false;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static CaddieWorkPattern createDefault(Caddie caddie, GolfCourse golfCourse) {
        CaddieWorkPattern pattern = new CaddieWorkPattern();
        pattern.caddie = caddie;
        pattern.golfCourse = golfCourse;
        pattern.canWeekday = true;
        pattern.canWeekend = true;
        pattern.periodLimit = null;
        pattern.isFirstWaitManual = false;
        pattern.isDeleted = false;
        return pattern;
    }

    public void update(Boolean canWeekday, Boolean canWeekend, PeriodLimit periodLimit, Boolean isFirstWaitManual) {
        this.canWeekday = canWeekday;
        this.canWeekend = canWeekend;
        this.periodLimit = periodLimit;
        this.isFirstWaitManual = isFirstWaitManual;
    }
}
