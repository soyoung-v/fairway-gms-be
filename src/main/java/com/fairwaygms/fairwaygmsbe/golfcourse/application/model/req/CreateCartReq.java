package com.fairwaygms.fairwaygmsbe.golfcourse.application.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 카트 등록 요청 (API-208). cartType 유효값(ELECTRIC/MANUAL)은 서비스 계층에서 검증한다.
public record CreateCartReq(
        // cart_number 컬럼은 VARCHAR(10) — 초과 입력이 DB 오류(500)로 새지 않도록 길이를 검증한다.
        @NotBlank(message = "카트 번호는 필수입니다.")
        @Size(max = 10, message = "카트 번호는 10자 이하여야 합니다.") String cartNumber,
        @NotBlank(message = "카트 타입은 필수입니다.") String cartType
) {
}
