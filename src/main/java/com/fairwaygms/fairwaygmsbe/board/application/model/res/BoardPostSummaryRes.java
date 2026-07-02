package com.fairwaygms.fairwaygmsbe.board.application.model.res;

import com.fairwaygms.fairwaygmsbe.board.domain.entity.BoardPost;

import java.time.LocalDateTime;

public record BoardPostSummaryRes(
        Long postId,
        String category,
        String title,
        LocalDateTime createdAt,
        long commentCount
) {
    public static BoardPostSummaryRes of(BoardPost p, long commentCount) {
        return new BoardPostSummaryRes(p.getId(), p.getCategory().name(),
                p.getTitle(), p.getCreatedAt(), commentCount);
    }
}
