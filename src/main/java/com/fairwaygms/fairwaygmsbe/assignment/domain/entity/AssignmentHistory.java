package com.fairwaygms.fairwaygmsbe.assignment.domain.entity;

import com.fairwaygms.fairwaygmsbe.assignment.domain.enums.AssignmentChangeType;
import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 배정 생성/수정/취소/교환/확정 모든 변경 이력 — NFR-010: 최소 1년 보존
@Getter
@Entity
@Table(
        name = "assignment_history",
        indexes = {
                @Index(name = "idx_assignment_history_assignment", columnList = "assignment_id"),
                @Index(name = "idx_assignment_history_change_type", columnList = "change_type"),
                @Index(name = "idx_assignment_history_created_at", columnList = "created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssignmentHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "golf_course_id", nullable = false)
    private GolfCourse golfCourse;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 30)
    private AssignmentChangeType changeType;

    // 최초 배정(AUTO/MANUAL), 확정/완료 이벤트는 beforeCaddie = null 가능
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "before_caddie_id")
    private Caddie beforeCaddie;

    // 취소 이벤트는 afterCaddie = null 가능
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "after_caddie_id")
    private Caddie afterCaddie;

    @Column(length = 255)
    private String reason;

    // 자동배정 포함 모든 변경의 처리자 — 자동배정은 요청한 Manager를 기록
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    public static AssignmentHistory record(Assignment assignment, GolfCourse golfCourse,
                                           AssignmentChangeType changeType,
                                           Caddie beforeCaddie, Caddie afterCaddie,
                                           String reason, User changedBy) {
        AssignmentHistory h = new AssignmentHistory();
        h.assignment = assignment;
        h.golfCourse = golfCourse;
        h.changeType = changeType;
        h.beforeCaddie = beforeCaddie;
        h.afterCaddie = afterCaddie;
        h.reason = reason;
        h.changedBy = changedBy;
        return h;
    }
}
