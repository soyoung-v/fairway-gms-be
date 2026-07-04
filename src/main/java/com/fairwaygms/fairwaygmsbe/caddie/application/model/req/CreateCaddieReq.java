package com.fairwaygms.fairwaygmsbe.caddie.application.model.req;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

// API-301: 캐디 직접 등록 요청 (FR-301) — 계정 없는 캐디 등록용
public record CreateCaddieReq(
        @NotBlank String caddieNumber,
        @NotBlank String name,
        String phone,
        LocalDate hireDate
) {
}
