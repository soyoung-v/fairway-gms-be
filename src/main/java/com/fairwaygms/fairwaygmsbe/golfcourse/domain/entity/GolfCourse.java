package com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity;

import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.enums.GolfCourseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 골프장 마스터 테이블. 전 도메인이 golf_course_id로 이 테이블을 참조한다.
@Getter
@Entity
@Table(
        name = "golf_course",
        indexes = {
                @Index(name = "idx_golf_course_status", columnList = "status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GolfCourse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String address;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GolfCourseStatus status = GolfCourseStatus.OPERATING;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 골프장 최초 등록
    public static GolfCourse create(String name, String address, String phone) {
        GolfCourse gc = new GolfCourse();
        gc.name = name;
        gc.address = address;
        gc.phone = phone;
        gc.status = GolfCourseStatus.OPERATING;
        gc.isDeleted = false;
        return gc;
    }

    // 골프장 기본 정보 수정
    public void update(String name, String address, String phone) {
        this.name = name;
        this.address = address;
        this.phone = phone;
    }
}
