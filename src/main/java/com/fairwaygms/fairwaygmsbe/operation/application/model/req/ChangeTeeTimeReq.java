package com.fairwaygms.fairwaygmsbe.operation.application.model.req;

import jakarta.validation.constraints.NotNull;

public record ChangeTeeTimeReq(
        @NotNull Long newTeeTimeId
) {}
