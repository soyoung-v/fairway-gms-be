package com.fairwaygms.fairwaygmsbe.assignment.domain.entity;

import com.fairwaygms.fairwaygmsbe.assignment.domain.enums.CartAssignmentStatus;
import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.Cart;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.TeeTime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 카트-티타임 배정 — 원본 명세는 UNIQUE(cart_id, assignment_date)였으나
// 같은 날 카트가 오전/오후 팀에 순차 배정되는 운영 케이스를 수용하기 위해
// tee_time_id를 추가하고 UNIQUE(cart_id, assignment_date, tee_time_id)로 변경
// (ADR-004 참조)
@Getter
@Entity
@Table(
        name = "cart_assignment",
        uniqueConstraints = {
                // 같은 날, 같은 티타임에 동일 카트 이중 배정 방지 — soft-delete와 혼용 불가하므로 서비스 레이어 보완 필요
                @UniqueConstraint(name = "uk_cart_assignment_date_tee_time",
                        columnNames = {"cart_id", "assignment_date", "tee_time_id"})
        },
        indexes = {
                @Index(name = "idx_cart_assignment_golf_course_date", columnList = "golf_course_id, assignment_date"),
                @Index(name = "idx_cart_assignment_cart_date", columnList = "cart_id, assignment_date")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartAssignment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "golf_course_id", nullable = false)
    private GolfCourse golfCourse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    // 카트 중복 배정 방지 키 구성요소 — 명세 확장 (ADR-004)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tee_time_id", nullable = false)
    private TeeTime teeTime;

    @Column(name = "assignment_date", nullable = false)
    private LocalDate assignmentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CartAssignmentStatus status = CartAssignmentStatus.ASSIGNED;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static CartAssignment create(GolfCourse golfCourse, Cart cart, TeeTime teeTime,
                                        LocalDate assignmentDate) {
        CartAssignment c = new CartAssignment();
        c.golfCourse = golfCourse;
        c.cart = cart;
        c.teeTime = teeTime;
        c.assignmentDate = assignmentDate;
        c.status = CartAssignmentStatus.ASSIGNED;
        c.isDeleted = false;
        return c;
    }

    // 카트 반납 처리 — 배정 해제와 구분하기 위해 RETURNED 상태 유지
    public void returnCart() {
        this.status = CartAssignmentStatus.RETURNED;
    }

    public void cancel() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
