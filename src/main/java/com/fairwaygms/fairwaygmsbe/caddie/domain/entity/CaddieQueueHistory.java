package com.fairwaygms.fairwaygmsbe.caddie.domain.entity;

import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.QueueChangeType;
import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 순번 초기화/수동 조정 이력. 수동 조정 시 reason 필수(FR-314)
@Getter
@Entity
@Table(
        name = "caddie_queue_history",
        indexes = {
                @Index(name = "idx_caddie_queue_history_caddie_date", columnList = "caddie_id, queue_date"),
                @Index(name = "idx_caddie_queue_history_change_type", columnList = "change_type")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaddieQueueHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caddie_id", nullable = false)
    private Caddie caddie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "golf_course_id", nullable = false)
    private GolfCourse golfCourse;

    @Column(name = "queue_date", nullable = false)
    private LocalDate queueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 30)
    private QueueChangeType changeType;

    @Column(name = "before_number")
    private Integer beforeNumber;

    @Column(name = "after_number", nullable = false)
    private Integer afterNumber;

    @Column(length = 255)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static CaddieQueueHistory record(Caddie caddie, GolfCourse golfCourse, LocalDate queueDate,
                                            QueueChangeType changeType, Integer beforeNumber,
                                            int afterNumber, String reason, User changedBy) {
        CaddieQueueHistory history = new CaddieQueueHistory();
        history.caddie = caddie;
        history.golfCourse = golfCourse;
        history.queueDate = queueDate;
        history.changeType = changeType;
        history.beforeNumber = beforeNumber;
        history.afterNumber = afterNumber;
        history.reason = reason;
        history.changedBy = changedBy;
        history.isDeleted = false;
        return history;
    }
}
