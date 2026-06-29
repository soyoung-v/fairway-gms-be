package com.fairwaygms.fairwaygmsbe.caddie.application.model.req;

import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.DailyStatusPriority;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.DailyStatusType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RegisterDailyStatusReq(
        @NotNull Long caddieId,
        @NotNull LocalDate statusDate,
        @NotNull DailyStatusType type,
        DailyStatusPriority priority,  // DUTY 타입 전용
        String note                    // SPECIAL 타입 설명 등
) {}
