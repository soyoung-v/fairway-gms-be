package com.fairwaygms.fairwaygmsbe.operation.application.model.res;

import com.fairwaygms.fairwaygmsbe.operation.domain.entity.RainCancellationPolicy;
import com.fairwaygms.fairwaygmsbe.operation.domain.enums.RainCancellationPolicyType;

public record RainPolicyRes(
        Long policyId,
        RainCancellationPolicyType policyType
) {
    public static RainPolicyRes from(RainCancellationPolicy policy) {
        return new RainPolicyRes(policy.getId(), policy.getPolicyType());
    }
}
