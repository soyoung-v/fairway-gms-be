package com.fairwaygms.fairwaygmsbe.golfcourse.application.model.req;

import jakarta.validation.constraints.NotBlank;

// 골프장 수정 요청 (API-202)
public record UpdateGolfCourseReq(
        @NotBlank(message = "골프장명은 필수입니다.") String name,
        String address,
        String phone
) {
}
