package com.fairwaygms.fairwaygmsbe.operation.application.model.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record CreateOperationSettingReq(
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}", message = "연월 형식은 YYYY-MM 이어야 합니다.")
        String yearMonth,

        @NotEmpty @Valid
        List<PeriodReq> periods
) {}
