package com.fairwaygms.fairwaygmsbe.operation.domain.entity;

import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.operation.domain.enums.RainCancellationPolicyType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "rain_cancellation_policy",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_rain_cancellation_policy_golf_course", columnNames = {"golf_course_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RainCancellationPolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 골프장당 1건
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "golf_course_id", nullable = false)
    private GolfCourse golfCourse;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", nullable = false, length = 30)
    private RainCancellationPolicyType policyType = RainCancellationPolicyType.KEEP_ORDER;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static RainCancellationPolicy create(GolfCourse golfCourse, RainCancellationPolicyType policyType) {
        RainCancellationPolicy policy = new RainCancellationPolicy();
        policy.golfCourse = golfCourse;
        policy.policyType = policyType;
        return policy;
    }

    public void updatePolicyType(RainCancellationPolicyType policyType) {
        this.policyType = policyType;
    }
}
