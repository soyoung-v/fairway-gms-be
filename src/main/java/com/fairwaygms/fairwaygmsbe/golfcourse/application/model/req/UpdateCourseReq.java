package com.fairwaygms.fairwaygmsbe.golfcourse.application.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// 코스 수정 요청 (API-206). holeCount 유효값(9/18/27)은 서비스 계층에서 검증한다.
public record UpdateCourseReq(
        @NotBlank(message = "코스명은 필수입니다.") String name,
        @NotNull(message = "홀 수는 필수입니다.") Integer holeCount,
        @NotNull(message = "표시 순서는 필수입니다.") Integer sortOrder,
        @NotNull(message = "운영 여부는 필수입니다.") Boolean isActive
) {
}
