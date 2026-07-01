package com.fairwaygms.fairwaygmsbe.board.domain.entity;

import com.fairwaygms.fairwaygmsbe.board.domain.enums.AuthorType;
import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(name = "board_comment", indexes = {
        @Index(name = "idx_board_comment_post_id", columnList = "post_id")
})
public class BoardComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "golf_course_id", nullable = false)
    private Long golfCourseId;

    @Column(name = "author_user_id", nullable = false)
    private Long authorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "author_type", nullable = false, length = 30)
    private AuthorType authorType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static BoardComment create(Long postId, Long golfCourseId,
                                      Long authorUserId, AuthorType authorType, String content) {
        BoardComment c = new BoardComment();
        c.postId = postId;
        c.golfCourseId = golfCourseId;
        c.authorUserId = authorUserId;
        c.authorType = authorType;
        c.content = content;
        c.isDeleted = false;
        return c;
    }

    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
