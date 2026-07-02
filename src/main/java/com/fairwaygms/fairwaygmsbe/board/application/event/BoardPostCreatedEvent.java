package com.fairwaygms.fairwaygmsbe.board.application.event;

import org.springframework.context.ApplicationEvent;

// 게시글 등록 이벤트 — AFTER_COMMIT 리스너에서 해당 골프장 캐디 전체 FCM 알림 발송에 사용
public class BoardPostCreatedEvent extends ApplicationEvent {

    private final Long postId;
    private final Long golfCourseId;
    private final String title;

    public BoardPostCreatedEvent(Object source, Long postId, Long golfCourseId, String title) {
        super(source);
        this.postId = postId;
        this.golfCourseId = golfCourseId;
        this.title = title;
    }

    public Long getPostId() { return postId; }
    public Long getGolfCourseId() { return golfCourseId; }
    public String getTitle() { return title; }
}
