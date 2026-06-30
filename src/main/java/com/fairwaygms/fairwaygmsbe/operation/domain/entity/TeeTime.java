package com.fairwaygms.fairwaygmsbe.operation.domain.entity;

import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.Course;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.operation.domain.enums.TeeTimeStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Entity
@Table(
        name = "tee_time",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tee_time_golf_course_date_time",
                        columnNames = {"golf_course_id", "course_id", "play_date", "start_time"})
        },
        indexes = {
                @Index(name = "idx_tee_time_golf_course_play_date", columnList = "golf_course_id, play_date")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeeTime extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "golf_course_id", nullable = false)
    private GolfCourse golfCourse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_period_id", nullable = false)
    private OperationPeriod operationPeriod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "play_date", nullable = false)
    private LocalDate playDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TeeTimeStatus status = TeeTimeStatus.OPEN;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static TeeTime create(GolfCourse golfCourse, OperationPeriod operationPeriod,
                                  Course course, LocalDate playDate, LocalTime startTime) {
        TeeTime teeTime = new TeeTime();
        teeTime.golfCourse = golfCourse;
        teeTime.operationPeriod = operationPeriod;
        teeTime.course = course;
        teeTime.playDate = playDate;
        teeTime.startTime = startTime;
        teeTime.status = TeeTimeStatus.OPEN;
        return teeTime;
    }

    public void close() {
        this.status = TeeTimeStatus.CLOSED;
    }

    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
