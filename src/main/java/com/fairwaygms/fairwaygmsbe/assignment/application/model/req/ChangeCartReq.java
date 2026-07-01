package com.fairwaygms.fairwaygmsbe.assignment.application.model.req;

import jakarta.validation.constraints.NotNull;

public record ChangeCartReq(@NotNull Long newCartId) {}
