package com.fairwaygms.fairwaygmsbe.caddie.domain.entity;

import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.DailyStatusPriority;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.DailyStatusType;
import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 동일 캐디/날짜에 복수 유형 등록 가능 (예: 조출+당번 동시 등록)
// 자동배정 시 DAY_OFF, ABSENCE, ASSIGN_EXCLUDED 유형이 있으면 배정 대상에서 제외
@Getter
@Entity
@Table(
        name = "caddie_daily_status",
        indexes = {
                @Index(name = "idx_caddie_daily_status_caddie_date", columnList = "caddie_id, status_date"),
                @Index(name = "idx_caddie_daily_status_type", columnList = "type")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaddieDailyStatus extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caddie_id", nullable = false)
    private Caddie caddie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "golf_course_id", nullable = false)
    private GolfCourse golfCourse;

    @Column(name = "status_date", nullable = false)
    private LocalDate statusDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DailyStatusType type;

    // 당번(DUTY) 유형일 때만 설정
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DailyStatusPriority priority;

    @Column(length = 255)
    private String note;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static CaddieDailyStatus create(Caddie caddie, GolfCourse golfCourse,
                                           LocalDate statusDate, DailyStatusType type,
                                           DailyStatusPriority priority, String note) {
        CaddieDailyStatus status = new CaddieDailyStatus();
        status.caddie = caddie;
        status.golfCourse = golfCourse;
        status.statusDate = statusDate;
        status.type = type;
        status.priority = priority;
        status.note = note;
        status.isDeleted = false;
        return status;
    }

    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
