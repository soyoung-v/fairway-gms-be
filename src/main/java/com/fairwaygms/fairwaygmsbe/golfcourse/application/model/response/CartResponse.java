package com.fairwaygms.fairwaygmsbe.golfcourse.application.model.response;

import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.Cart;

// 카트 응답 DTO
public record CartResponse(
        Long cartId,
        String cartNumber,
        String cartType,
        String status,
        String note
) {
    // 엔티티로부터 응답 DTO 생성
    public static CartResponse from(Cart cart) {
        return new CartResponse(
                cart.getId(),
                cart.getCartNumber(),
                cart.getCartType().name(),
                cart.getStatus().name(),
                cart.getNote()
        );
    }
}
