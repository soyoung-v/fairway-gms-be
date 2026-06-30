package com.fairwaygms.fairwaygmsbe.operation.application.model.req;

import com.fairwaygms.fairwaygmsbe.operation.domain.enums.RainCancellationPolicyType;
import jakarta.validation.constraints.NotNull;

public record UpdateRainPolicyReq(
        @NotNull RainCancellationPolicyType policyType
) {}
