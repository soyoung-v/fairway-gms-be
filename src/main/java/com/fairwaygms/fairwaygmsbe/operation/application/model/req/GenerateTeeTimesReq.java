package com.fairwaygms.fairwaygmsbe.operation.application.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record GenerateTeeTimesReq(
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}", message = "연월 형식은 YYYY-MM 이어야 합니다.")
        String yearMonth,

        // 비어있으면 운영 설정의 전체 코스 대상
        List<Long> courseIds
) {}
