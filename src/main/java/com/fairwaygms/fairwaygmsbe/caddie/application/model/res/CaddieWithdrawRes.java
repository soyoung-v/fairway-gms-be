package com.fairwaygms.fairwaygmsbe.caddie.application.model.res;

import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;

public record CaddieWithdrawRes(
        Long caddieId,
        String name,
        String status
) {
    public static CaddieWithdrawRes from(Caddie caddie) {
        return new CaddieWithdrawRes(
                caddie.getId(),
                caddie.getName(),
                caddie.getStatus().getLabel()
        );
    }
}
