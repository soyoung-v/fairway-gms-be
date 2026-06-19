package com.fairwaygms.fairwaygmsbe.golfcourse.application.model.request;

import jakarta.validation.constraints.NotBlank;

// 골프장 등록 요청 (API-201)
public record CreateGolfCourseRequest(
        @NotBlank(message = "골프장명은 필수입니다.") String name,
        String address,
        String phone
) {
}
