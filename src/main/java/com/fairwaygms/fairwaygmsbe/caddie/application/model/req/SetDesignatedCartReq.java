package com.fairwaygms.fairwaygmsbe.caddie.application.model.req;

import jakarta.validation.constraints.NotNull;

public record SetDesignatedCartReq(
        @NotNull
        Long cartId
) {}
