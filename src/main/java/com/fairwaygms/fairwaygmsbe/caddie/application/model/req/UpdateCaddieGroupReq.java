package com.fairwaygms.fairwaygmsbe.caddie.application.model.req;

import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.CaddieGroupAssignmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCaddieGroupReq(
        @NotBlank @Size(max = 50) String name,
        @NotNull CaddieGroupAssignmentType assignmentType
) {}
