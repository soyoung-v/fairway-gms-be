package com.fairwaygms.fairwaygmsbe.golfcourse.application.model.req;

import jakarta.validation.constraints.NotBlank;

// 카트 상태 변경 요청 (API-211). 유효값(AVAILABLE/MAINTENANCE/DISABLED)은 서비스 계층에서 검증한다.
public record UpdateCartStatusReq(
        @NotBlank(message = "상태값은 필수입니다.") String status
) {
}
