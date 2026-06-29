package com.fairwaygms.fairwaygmsbe.caddie.application.model.res;

import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieDesignatedCart;

public record DesignatedCartRes(
        Long caddieId,
        String caddieName,
        Long cartId,
        String cartNumber
) {
    public static DesignatedCartRes from(CaddieDesignatedCart dc) {
        return new DesignatedCartRes(
                dc.getCaddie().getId(),
                dc.getCaddie().getName(),
                dc.getCart().getId(),
                dc.getCart().getCartNumber()
        );
    }
}
