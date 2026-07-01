package com.fairwaygms.fairwaygmsbe.board.domain.repository;

import com.fairwaygms.fairwaygmsbe.board.domain.entity.BoardComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardCommentRepository extends JpaRepository<BoardComment, Long> {

    Page<BoardComment> findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(Long postId, Pageable pageable);

    long countByPostIdAndIsDeletedFalse(Long postId);
}
