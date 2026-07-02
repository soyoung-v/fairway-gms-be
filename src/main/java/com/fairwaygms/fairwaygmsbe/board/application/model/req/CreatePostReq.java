package com.fairwaygms.fairwaygmsbe.board.application.model.req;

import com.fairwaygms.fairwaygmsbe.board.domain.enums.PostCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePostReq(
        @NotNull PostCategory category,
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content
) {}
