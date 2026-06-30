package com.fairwaygms.fairwaygmsbe.assignment.domain.entity;

import com.fairwaygms.fairwaygmsbe.assignment.domain.enums.AssignmentStatus;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.ReservationTeam;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 소프트 삭제 구조에서 DB UNIQUE(reservation_team_id)는 사용할 수 없다 (삭제 행이 제약을 점유).
// 중복 방지는 서비스 레이어의 existsByReservationTeam_IdAndIsDeletedFalse()로 처리한다.
@Getter
@Entity
@Table(
        name = "assignment",
        indexes = {
                @Index(name = "idx_assignment_golf_course_date", columnList = "golf_course_id, assignment_date"),
                @Index(name = "idx_assignment_caddie_date", columnList = "caddie_id, assignment_date")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Assignment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "golf_course_id", nullable = false)
    private GolfCourse golfCourse;

    // 예약팀당 활성 배정은 1건 — 중복 방지는 서비스 레이어에서 isDeleted=false 기준으로 검증
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_team_id", nullable = false)
    private ReservationTeam reservationTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caddie_id", nullable = false)
    private Caddie caddie;

    @Column(name = "assignment_date", nullable = false)
    private LocalDate assignmentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssignmentStatus status = AssignmentStatus.ASSIGNED;

    // 지정 캐디 배정 시 true — Manager의 강제 해제(FR-512) 전까지 자동재배정 불가
    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;

    // 캐디 1명이 2팀을 담당하는 하프백 배정 (FR-506)
    @Column(name = "is_half_back", nullable = false)
    private Boolean isHalfBack = false;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static Assignment create(GolfCourse golfCourse, ReservationTeam reservationTeam,
                                    Caddie caddie, LocalDate assignmentDate,
                                    boolean isLocked, boolean isHalfBack) {
        Assignment a = new Assignment();
        a.golfCourse = golfCourse;
        a.reservationTeam = reservationTeam;
        a.caddie = caddie;
        a.assignmentDate = assignmentDate;
        a.status = AssignmentStatus.ASSIGNED;
        a.isLocked = isLocked;
        a.isHalfBack = isHalfBack;
        a.isDeleted = false;
        return a;
    }

    // 재배정 — 잠금 해제 후 새 캐디로 교체. 상태는 ASSIGNED로 초기화
    public void reassign(Caddie newCaddie) {
        this.caddie = newCaddie;
        this.isLocked = false;
        this.status = AssignmentStatus.ASSIGNED;
    }

    // 배정표 확정 — AssignmentConfirmedEvent 발행은 DailyScheduleService에서 처리
    public void confirm() {
        this.status = AssignmentStatus.CONFIRMED;
    }

    // 확정 취소 — CONFIRMED → ASSIGNED 복귀
    public void cancelConfirm() {
        this.status = AssignmentStatus.ASSIGNED;
    }

    // 배정 취소 — 소프트 삭제로 isDeleted=true 처리해야 같은 팀 재배정이 가능
    public void cancel() {
        this.status = AssignmentStatus.CANCELLED;
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    // 라운딩 완료 — AssignmentCompletedEvent 발행은 DailyScheduleService에서 처리
    public void complete() {
        this.status = AssignmentStatus.COMPLETED;
    }

    // 잠금 강제 해제 (FR-512) — 해제 사유는 AssignmentHistory에 기록
    public void unlock() {
        this.isLocked = false;
    }
}
