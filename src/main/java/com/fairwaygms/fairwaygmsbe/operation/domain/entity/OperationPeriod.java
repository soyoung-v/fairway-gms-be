package com.fairwaygms.fairwaygmsbe.operation.domain.entity;

import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.Course;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Entity
@Table(
        name = "operation_period",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_operation_period_setting_course_number",
                        columnNames = {"operation_setting_id", "course_id", "period_number"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OperationPeriod extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_setting_id", nullable = false)
    private OperationSetting operationSetting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "golf_course_id", nullable = false)
    private GolfCourse golfCourse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    // 1=1부, 2=2부, 3=3부
    @Column(name = "period_number", nullable = false)
    private Integer periodNumber;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "tee_time_interval", nullable = false)
    private Integer teeTimeInterval;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static OperationPeriod create(OperationSetting operationSetting, GolfCourse golfCourse,
                                         Course course, int periodNumber,
                                         LocalTime startTime, LocalTime endTime, int teeTimeInterval) {
        OperationPeriod period = new OperationPeriod();
        period.operationSetting = operationSetting;
        period.golfCourse = golfCourse;
        period.course = course;
        period.periodNumber = periodNumber;
        period.startTime = startTime;
        period.endTime = endTime;
        period.teeTimeInterval = teeTimeInterval;
        period.isActive = true;
        return period;
    }

    public void update(LocalTime startTime, LocalTime endTime, int teeTimeInterval, boolean isActive) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.teeTimeInterval = teeTimeInterval;
        this.isActive = isActive;
    }
}
