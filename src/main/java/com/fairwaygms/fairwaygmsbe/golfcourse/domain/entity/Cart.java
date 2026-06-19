package com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity;

import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.enums.CartStatus;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.enums.CartType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

// 골프장 보유 카트 목록. 카트 자동배정과 반납 처리의 기준이 된다.
@Getter
@Entity
@Table(
        name = "cart",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cart_golf_course_number", columnNames = {"golf_course_id", "cart_number"})
        },
        indexes = {
                @Index(name = "idx_cart_status", columnList = "status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "golf_course_id", nullable = false)
    private GolfCourse golfCourse;

    @Column(name = "cart_number", nullable = false, length = 10)
    private String cartNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "cart_type", nullable = false, length = 30)
    private CartType cartType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CartStatus status = CartStatus.AVAILABLE;

    @Column(length = 255)
    private String note;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 카트 최초 등록 — 초기 상태는 AVAILABLE
    public static Cart create(GolfCourse golfCourse, String cartNumber, CartType cartType) {
        Cart cart = new Cart();
        cart.golfCourse = golfCourse;
        cart.cartNumber = cartNumber;
        cart.cartType = cartType;
        cart.status = CartStatus.AVAILABLE;
        cart.isDeleted = false;
        return cart;
    }

    // 카트 정보 수정 (번호, 타입, 비고)
    public void update(String cartNumber, CartType cartType, String note) {
        this.cartNumber = cartNumber;
        this.cartType = cartType;
        this.note = note;
    }

    // 카트 상태 변경 (점검, 사용중지 등)
    public void changeStatus(CartStatus status) {
        this.status = status;
    }

    // 배정 완료 이벤트 수신 시 카트를 사용 가능 상태로 복원
    public void markAvailable() {
        this.status = CartStatus.AVAILABLE;
    }
}
