package com.fairwaygms.fairwaygmsbe.board.application.model.req;

import jakarta.validation.constraints.NotBlank;

public record RejectSwapRequestReq(
        @NotBlank String rejectReason
) {}
