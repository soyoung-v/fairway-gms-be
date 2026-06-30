package com.fairwaygms.fairwaygmsbe.assignment.domain.entity;

import com.fairwaygms.fairwaygmsbe.assignment.domain.enums.DailyScheduleStatus;
import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 일별 배정표 — 확정(CONFIRMED) 시점에 AssignmentConfirmedEvent를 발행해 정산/알림에 연동한다
@Getter
@Entity
@Table(
        name = "daily_schedule",
        // 골프장+날짜 조합은 활성 행 기준으로 중복 방지 (soft-delete 구조이므로 DB UNIQUE 대신 서비스 검증)
        indexes = {
                @Index(name = "idx_daily_schedule_golf_course_date", columnList = "golf_course_id, schedule_date"),
                @Index(name = "idx_daily_schedule_status", columnList = "status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailySchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "golf_course_id", nullable = false)
    private GolfCourse golfCourse;

    @Column(name = "schedule_date", nullable = false)
    private LocalDate scheduleDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DailyScheduleStatus status = DailyScheduleStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private User confirmedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static DailySchedule create(GolfCourse golfCourse, LocalDate scheduleDate) {
        DailySchedule d = new DailySchedule();
        d.golfCourse = golfCourse;
        d.scheduleDate = scheduleDate;
        d.status = DailyScheduleStatus.DRAFT;
        d.isDeleted = false;
        return d;
    }

    // 배정표 확정 — 이후 개별 배정 취소/수정은 Manager 권한으로만 가능
    public void confirm(User confirmedBy) {
        this.status = DailyScheduleStatus.CONFIRMED;
        this.confirmedBy = confirmedBy;
        this.confirmedAt = LocalDateTime.now();
    }

    // 확정 취소 — 재편성이 필요할 때 DRAFT로 복귀
    public void cancelConfirm() {
        this.status = DailyScheduleStatus.DRAFT;
        this.confirmedBy = null;
        this.confirmedAt = null;
    }
}
