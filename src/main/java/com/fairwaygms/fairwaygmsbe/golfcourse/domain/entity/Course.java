package com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity;

import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 골프장 내 코스 정보. 홀 수는 캐디피 계산 기준으로 사용된다.
@Getter
@Entity
@Table(
        name = "course",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_course_golf_course_name", columnNames = {"golf_course_id", "name"})
        },
        indexes = {
                @Index(name = "idx_course_is_active", columnList = "is_active")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "golf_course_id", nullable = false)
    private GolfCourse golfCourse;

    @Column(nullable = false, length = 50)
    private String name;

    // 홀 수: 9, 18, 27 중 하나 — 캐디피 계산 기준
    @Column(name = "hole_count", nullable = false)
    private int holeCount;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 코스 최초 등록
    public static Course create(GolfCourse golfCourse, String name, int holeCount, int sortOrder) {
        Course course = new Course();
        course.golfCourse = golfCourse;
        course.name = name;
        course.holeCount = holeCount;
        course.sortOrder = sortOrder;
        course.isActive = true;
        course.isDeleted = false;
        return course;
    }

    // 코스 정보 수정 (이름, 홀 수, 표시 순서, 운영 여부)
    public void update(String name, int holeCount, int sortOrder, Boolean isActive) {
        this.name = name;
        this.holeCount = holeCount;
        this.sortOrder = sortOrder;
        this.isActive = isActive;
    }
}
