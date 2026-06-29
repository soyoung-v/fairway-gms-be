package com.fairwaygms.fairwaygmsbe.caddie.application.model.res;

import java.time.LocalDate;

public record MyQueueRes(
        Integer queueNumber,
        LocalDate queueDate
) {
    public static MyQueueRes of(Integer queueNumber, LocalDate queueDate) {
        return new MyQueueRes(queueNumber, queueDate);
    }
}
