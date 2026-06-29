package com.fairwaygms.fairwaygmsbe.caddie.application.model.res;

import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;

public record AvailableCaddieRes(
        Long id,
        String caddieNumber,
        String name,
        Integer queueNumber
) {
    public static AvailableCaddieRes of(Caddie caddie, Integer queueNumber) {
        return new AvailableCaddieRes(
                caddie.getId(),
                caddie.getCaddieNumber(),
                caddie.getName(),
                queueNumber
        );
    }
}
