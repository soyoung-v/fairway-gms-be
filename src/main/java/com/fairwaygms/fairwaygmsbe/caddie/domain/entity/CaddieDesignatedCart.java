package com.fairwaygms.fairwaygmsbe.caddie.domain.entity;

import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.Cart;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 동일 caddie-cart 중복은 UNIQUE 제약으로 막고,
// is_active=1인 지정카트가 캐디당 1건만 유지되도록 Service 계층에서 검증
@Getter
@Entity
@Table(
        name = "caddie_designated_cart",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_caddie_designated_cart", columnNames = {"caddie_id", "cart_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaddieDesignatedCart extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caddie_id", nullable = false)
    private Caddie caddie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "golf_course_id", nullable = false)
    private GolfCourse golfCourse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static CaddieDesignatedCart create(Caddie caddie, GolfCourse golfCourse, Cart cart) {
        CaddieDesignatedCart designated = new CaddieDesignatedCart();
        designated.caddie = caddie;
        designated.golfCourse = golfCourse;
        designated.cart = cart;
        designated.isActive = true;
        designated.isDeleted = false;
        return designated;
    }

    public void deactivate() {
        this.isActive = false;
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
