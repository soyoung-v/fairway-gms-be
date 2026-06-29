package com.fairwaygms.fairwaygmsbe.caddie.application.model.req;

import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.CaddieStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeCaddieStatusReq(
        @NotNull
        CaddieStatus status
) {}
