package com.fairwaygms.fairwaygmsbe.board.application.model.res;

import com.fairwaygms.fairwaygmsbe.board.domain.entity.BoardPost;

import java.time.LocalDateTime;

public record BoardPostRes(
        Long postId,
        String category,
        String title,
        String content,
        LocalDateTime createdAt
) {
    public static BoardPostRes from(BoardPost p) {
        return new BoardPostRes(p.getId(), p.getCategory().name(),
                p.getTitle(), p.getContent(), p.getCreatedAt());
    }
}
