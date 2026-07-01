package com.fairwaygms.fairwaygmsbe.board.domain.entity;

import com.fairwaygms.fairwaygmsbe.board.domain.enums.PostCategory;
import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(name = "board_post", indexes = {
        @Index(name = "idx_board_post_golf_course_category", columnList = "golf_course_id, category"),
        @Index(name = "idx_board_post_created_at", columnList = "created_at")
})
public class BoardPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "golf_course_id", nullable = false)
    private Long golfCourseId;

    @Column(name = "author_user_id", nullable = false)
    private Long authorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PostCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static BoardPost create(Long golfCourseId, Long authorUserId,
                                   PostCategory category, String title, String content) {
        BoardPost p = new BoardPost();
        p.golfCourseId = golfCourseId;
        p.authorUserId = authorUserId;
        p.category = category;
        p.title = title;
        p.content = content;
        p.isDeleted = false;
        return p;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
