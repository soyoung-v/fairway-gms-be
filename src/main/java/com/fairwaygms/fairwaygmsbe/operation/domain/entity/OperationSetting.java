package com.fairwaygms.fairwaygmsbe.operation.domain.entity;

import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "operation_setting",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_operation_setting_golf_course_year_month", columnNames = {"golf_course_id", "year_month"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OperationSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "golf_course_id", nullable = false)
    private GolfCourse golfCourse;

    // 예: 2025-06
    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static OperationSetting create(GolfCourse golfCourse, String yearMonth) {
        OperationSetting setting = new OperationSetting();
        setting.golfCourse = golfCourse;
        setting.yearMonth = yearMonth;
        return setting;
    }
}
