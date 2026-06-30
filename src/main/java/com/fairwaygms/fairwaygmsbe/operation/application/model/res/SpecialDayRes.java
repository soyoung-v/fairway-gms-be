package com.fairwaygms.fairwaygmsbe.operation.application.model.res;

import com.fairwaygms.fairwaygmsbe.operation.domain.entity.SpecialOperationDay;

import java.time.LocalDate;

public record SpecialDayRes(
        Long specialDayId,
        LocalDate operationDate,
        String note
) {
    public static SpecialDayRes from(SpecialOperationDay day) {
        return new SpecialDayRes(day.getId(), day.getOperationDate(), day.getNote());
    }
}
