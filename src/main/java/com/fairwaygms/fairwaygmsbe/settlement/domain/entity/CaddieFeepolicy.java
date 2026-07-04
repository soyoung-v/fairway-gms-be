package com.fairwaygms.fairwaygmsbe.settlement.domain.entity;

import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import com.fairwaygms.fairwaygmsbe.settlement.domain.enums.HalfBackType;
import com.fairwaygms.fairwaygmsbe.settlement.domain.enums.NoShowPolicy;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(name = "caddie_fee_policy", uniqueConstraints = {
        @UniqueConstraint(name = "uq_caddie_fee_policy_golf_course", columnNames = "golf_course_id")
})
public class CaddieFeepolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "golf_course_id", nullable = false, unique = true)
    private Long golfCourseId;

    @Column(name = "full_round_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal fullRoundFee;

    @Column(name = "half_round_fee", precision = 10, scale = 2)
    private BigDecimal halfRoundFee;

    @Enumerated(EnumType.STRING)
    @Column(name = "half_back_type", length = 30)
    private HalfBackType halfBackType;

    @Enumerated(EnumType.STRING)
    @Column(name = "no_show_policy", nullable = false, length = 30)
    private NoShowPolicy noShowPolicy = NoShowPolicy.NONE;

    @Column(name = "no_show_fee", precision = 10, scale = 2)
    private BigDecimal noShowFee;

    // 5인 플레이 팀 추가 캐디피 — 2~4인은 동일, 5인 이상 팀만 기본 캐디피에 가산
    @Column(name = "extra_player_fee", precision = 10, scale = 2)
    private BigDecimal extraPlayerFee;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static CaddieFeepolicy create(Long golfCourseId, BigDecimal fullRoundFee,
                                         BigDecimal halfRoundFee, HalfBackType halfBackType,
                                         NoShowPolicy noShowPolicy, BigDecimal noShowFee,
                                         BigDecimal extraPlayerFee) {
        CaddieFeepolicy p = new CaddieFeepolicy();
        p.golfCourseId = golfCourseId;
        p.fullRoundFee = fullRoundFee;
        p.halfRoundFee = halfRoundFee;
        p.halfBackType = halfBackType;
        p.noShowPolicy = noShowPolicy != null ? noShowPolicy : NoShowPolicy.NONE;
        p.noShowFee = noShowFee;
        p.extraPlayerFee = extraPlayerFee;
        p.isDeleted = false;
        return p;
    }

    public void update(BigDecimal fullRoundFee, BigDecimal halfRoundFee,
                       HalfBackType halfBackType, NoShowPolicy noShowPolicy, BigDecimal noShowFee,
                       BigDecimal extraPlayerFee) {
        this.fullRoundFee = fullRoundFee;
        this.halfRoundFee = halfRoundFee;
        this.halfBackType = halfBackType;
        this.noShowPolicy = noShowPolicy != null ? noShowPolicy : NoShowPolicy.NONE;
        this.noShowFee = noShowFee;
        this.extraPlayerFee = extraPlayerFee;
    }
}
