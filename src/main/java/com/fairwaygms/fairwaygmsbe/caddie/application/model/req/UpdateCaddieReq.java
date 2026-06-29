package com.fairwaygms.fairwaygmsbe.caddie.application.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateCaddieReq(
        @NotBlank @Size(max = 20)
        String caddieNumber,

        @Size(max = 20)
        String phone,

        LocalDate hireDate
) {}
