package com.fairwaygms.fairwaygmsbe.golfcourse.application.model.res;

import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.Cart;

// 카트 응답 DTO
public record CartRes(
        Long cartId,
        String cartNumber,
        String cartType,
        String status,
        String note
) {
    // 엔티티로부터 응답 DTO 생성
    public static CartRes from(Cart cart) {
        return new CartRes(
                cart.getId(),
                cart.getCartNumber(),
                cart.getCartType().name(),
                cart.getStatus().name(),
                cart.getNote()
        );
    }
}
