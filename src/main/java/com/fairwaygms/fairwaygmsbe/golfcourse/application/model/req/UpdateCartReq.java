package com.fairwaygms.fairwaygmsbe.golfcourse.application.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 카트 수정 요청 (API-210). cartType 유효값(ELECTRIC/MANUAL)은 서비스 계층에서 검증한다.
public record UpdateCartReq(
        // cart_number 컬럼은 VARCHAR(10) — 등록과 동일하게 길이를 검증한다.
        @NotBlank(message = "카트 번호는 필수입니다.")
        @Size(max = 10, message = "카트 번호는 10자 이하여야 합니다.") String cartNumber,
        @NotBlank(message = "카트 타입은 필수입니다.") String cartType,
        String note
) {
}
