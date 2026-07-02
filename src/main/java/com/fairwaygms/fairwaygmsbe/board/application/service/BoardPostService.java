package com.fairwaygms.fairwaygmsbe.board.application.service;

import com.fairwaygms.fairwaygmsbe.board.application.event.BoardPostCreatedEvent;
import com.fairwaygms.fairwaygmsbe.board.application.model.req.CreatePostReq;
import com.fairwaygms.fairwaygmsbe.board.application.model.req.UpdatePostReq;
import com.fairwaygms.fairwaygmsbe.board.application.model.res.BoardPostRes;
import com.fairwaygms.fairwaygmsbe.board.application.model.res.BoardPostSummaryRes;
import com.fairwaygms.fairwaygmsbe.board.domain.entity.BoardPost;
import com.fairwaygms.fairwaygmsbe.board.domain.enums.PostCategory;
import com.fairwaygms.fairwaygmsbe.board.domain.repository.BoardCommentRepository;
import com.fairwaygms.fairwaygmsbe.board.domain.repository.BoardPostRepository;
import com.fairwaygms.fairwaygmsbe.board.exception.BoardErrorCode;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardPostService {

    private final BoardPostRepository boardPostRepository;
    private final BoardCommentRepository boardCommentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public BoardPostRes createPost(CreatePostReq req, AuthenticatedUser auth) {
        BoardPost post = boardPostRepository.save(
                BoardPost.create(auth.getGolfCourseId(), auth.getUserId(),
                        req.category(), req.title(), req.content()));

        eventPublisher.publishEvent(
                new BoardPostCreatedEvent(this, post.getId(), auth.getGolfCourseId(), post.getTitle()));

        return BoardPostRes.from(post);
    }

    @Transactional
    public BoardPostRes updatePost(Long postId, UpdatePostReq req, AuthenticatedUser auth) {
        BoardPost post = getActivePost(postId);
        validateAuthor(post, auth);
        post.update(req.title(), req.content());
        return BoardPostRes.from(post);
    }

    @Transactional
    public void deletePost(Long postId, AuthenticatedUser auth) {
        BoardPost post = getActivePost(postId);
        validateGolfCourse(post, auth);
        post.delete();
    }

    @Transactional(readOnly = true)
    public Page<BoardPostSummaryRes> getPosts(PostCategory category, int page, int size,
                                              AuthenticatedUser auth) {
        return boardPostRepository
                .findByGolfCourseAndCategory(auth.getGolfCourseId(), category, PageRequest.of(page, size))
                .map(p -> BoardPostSummaryRes.of(p,
                        boardCommentRepository.countByPostIdAndIsDeletedFalse(p.getId())));
    }

    @Transactional(readOnly = true)
    public BoardPostRes getPost(Long postId, AuthenticatedUser auth) {
        BoardPost post = getActivePost(postId);
        validateGolfCourse(post, auth);
        return BoardPostRes.from(post);
    }

    // ─── 내부 헬퍼 ──────────────────────────────────────────────────────────────

    private BoardPost getActivePost(Long postId) {
        return boardPostRepository.findById(postId)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new BusinessException(BoardErrorCode.POST_NOT_FOUND));
    }

    private void validateAuthor(BoardPost post, AuthenticatedUser auth) {
        if (!post.getAuthorUserId().equals(auth.getUserId())) {
            throw new BusinessException(BoardErrorCode.POST_ACCESS_DENIED);
        }
    }

    private void validateGolfCourse(BoardPost post, AuthenticatedUser auth) {
        if (!post.getGolfCourseId().equals(auth.getGolfCourseId())) {
            throw new BusinessException(BoardErrorCode.POST_ACCESS_DENIED);
        }
    }
}
