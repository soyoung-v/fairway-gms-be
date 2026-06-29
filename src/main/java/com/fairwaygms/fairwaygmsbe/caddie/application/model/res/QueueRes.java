package com.fairwaygms.fairwaygmsbe.caddie.application.model.res;

import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieQueue;

import java.time.LocalDate;

public record QueueRes(
        Long caddieId,
        String caddieName,
        String caddieNumber,
        Integer queueNumber,
        LocalDate queueDate
) {
    public static QueueRes from(CaddieQueue queue) {
        return new QueueRes(
                queue.getCaddie().getId(),
                queue.getCaddie().getName(),
                queue.getCaddie().getCaddieNumber(),
                queue.getQueueNumber(),
                queue.getQueueDate()
        );
    }
}
