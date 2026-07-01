package com.fairwaygms.fairwaygmsbe.assignment.application.model.req;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CartAssignReq(
        @NotNull Long cartId,
        @NotNull Long teeTimeId,
        @NotNull LocalDate assignmentDate
) {}
