package com.fairwaygms.fairwaygmsbe.board.application.model.res;

import com.fairwaygms.fairwaygmsbe.board.domain.entity.BoardComment;

import java.time.LocalDateTime;

public record BoardCommentRes(
        Long commentId,
        String content,
        String authorType,
        LocalDateTime createdAt
) {
    public static BoardCommentRes from(BoardComment c) {
        return new BoardCommentRes(c.getId(), c.getContent(),
                c.getAuthorType().name(), c.getCreatedAt());
    }
}
