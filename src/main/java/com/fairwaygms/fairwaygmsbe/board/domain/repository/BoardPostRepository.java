package com.fairwaygms.fairwaygmsbe.board.domain.repository;

import com.fairwaygms.fairwaygmsbe.board.domain.entity.BoardPost;
import com.fairwaygms.fairwaygmsbe.board.domain.enums.PostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardPostRepository extends JpaRepository<BoardPost, Long> {

    @Query("SELECT p FROM BoardPost p WHERE p.golfCourseId = :golfCourseId " +
           "AND p.isDeleted = false " +
           "AND (:category IS NULL OR p.category = :category) " +
           "ORDER BY p.createdAt DESC")
    Page<BoardPost> findByGolfCourseAndCategory(@Param("golfCourseId") Long golfCourseId,
                                                @Param("category") PostCategory category,
                                                Pageable pageable);
}
