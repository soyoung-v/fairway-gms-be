package com.fairwaygms.fairwaygmsbe.golfcourse.application.model.request;

import jakarta.validation.constraints.NotBlank;

// 카트 수정 요청 (API-210). cartType 유효값(ELECTRIC/MANUAL)은 서비스 계층에서 검증한다.
public record UpdateCartRequest(
        @NotBlank(message = "카트 번호는 필수입니다.") String cartNumber,
        @NotBlank(message = "카트 타입은 필수입니다.") String cartType,
        String note
) {
}
