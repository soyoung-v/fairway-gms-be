package com.fairwaygms.fairwaygmsbe.caddie.application.model.res;

import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieDailyStatus;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.DailyStatusPriority;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.DailyStatusType;

import java.time.LocalDate;

public record DailyStatusRes(
        Long statusId,
        Long caddieId,
        LocalDate statusDate,
        DailyStatusType type,
        String typeLabel,
        DailyStatusPriority priority,
        String note
) {
    public static DailyStatusRes from(CaddieDailyStatus status) {
        return new DailyStatusRes(
                status.getId(),
                status.getCaddie().getId(),
                status.getStatusDate(),
                status.getType(),
                status.getType().getLabel(),
                status.getPriority(),
                status.getNote()
        );
    }
}
