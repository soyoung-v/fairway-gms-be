package com.fairwaygms.fairwaygmsbe.operation.domain.entity;

import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "special_operation_day",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_special_operation_day_golf_course_date",
                        columnNames = {"golf_course_id", "operation_date"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpecialOperationDay extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "golf_course_id", nullable = false)
    private GolfCourse golfCourse;

    @Column(name = "operation_date", nullable = false)
    private LocalDate operationDate;

    @Column(length = 255)
    private String note;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static SpecialOperationDay create(GolfCourse golfCourse, LocalDate operationDate, String note) {
        SpecialOperationDay day = new SpecialOperationDay();
        day.golfCourse = golfCourse;
        day.operationDate = operationDate;
        day.note = note;
        return day;
    }

    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
