package com.fairwaygms.fairwaygmsbe.caddie.domain.entity;

import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.CaddieGroupAssignmentType;
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
        name = "caddie_group",
        indexes = {
                @Index(name = "idx_caddie_group_golf_course", columnList = "golf_course_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaddieGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "golf_course_id", nullable = false)
    private GolfCourse golfCourse;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", nullable = false, length = 30)
    private CaddieGroupAssignmentType assignmentType;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 골프장 등록 시 기본 하우스 그룹 자동 생성에 사용
    public static CaddieGroup createDefault(GolfCourse golfCourse) {
        CaddieGroup group = new CaddieGroup();
        group.golfCourse = golfCourse;
        group.name = "하우스캐디";
        group.assignmentType = CaddieGroupAssignmentType.HOUSE;
        group.isDeleted = false;
        return group;
    }

    public static CaddieGroup create(GolfCourse golfCourse, String name, CaddieGroupAssignmentType assignmentType) {
        CaddieGroup group = new CaddieGroup();
        group.golfCourse = golfCourse;
        group.name = name;
        group.assignmentType = assignmentType;
        group.isDeleted = false;
        return group;
    }

    public void update(String name, CaddieGroupAssignmentType assignmentType) {
        this.name = name;
        this.assignmentType = assignmentType;
    }

    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
