package com.fairwaygms.fairwaygmsbe.assignment.application.model.req;

import jakarta.validation.constraints.NotNull;

public record ReassignReq(
        @NotNull Long newCaddieId,
        String reason
) {}
