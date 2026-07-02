package com.fairwaygms.fairwaygmsbe.board.application.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePostReq(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content
) {}
