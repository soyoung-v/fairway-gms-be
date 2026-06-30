package com.fairwaygms.fairwaygmsbe.operation.application.model.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateOperationSettingReq(
        @NotEmpty @Valid
        List<UpdatePeriodReq> periods
) {}
