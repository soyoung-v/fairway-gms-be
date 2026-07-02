package com.fairwaygms.fairwaygmsbe.board.application.model.req;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateSwapRequestReq(
        @NotNull Long targetCaddieId,
        @NotNull LocalDate requestDate,
        String reason
) {}
