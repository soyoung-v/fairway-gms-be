package com.fairwaygms.fairwaygmsbe.operation.application.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BulkRegenerateTeeTimesReq(
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}", message = "연월 형식은 YYYY-MM 이어야 합니다.")
        String yearMonth,

        // false이면 팀 없는 기존 티타임 삭제 후 재생성
        Boolean preserveTeams
) {}
