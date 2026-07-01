package com.fairwaygms.fairwaygmsbe.settlement.application.model.req;

import jakarta.validation.constraints.NotNull;

public record NoShowCalculateReq(
        @NotNull Long assignmentId
) {}
