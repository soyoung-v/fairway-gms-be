package com.fairwaygms.fairwaygmsbe.board.application.service;

import com.fairwaygms.fairwaygmsbe.board.application.model.req.CreateCommentReq;
import com.fairwaygms.fairwaygmsbe.board.application.model.res.BoardCommentRes;
import com.fairwaygms.fairwaygmsbe.board.domain.entity.BoardComment;
import com.fairwaygms.fairwaygmsbe.board.domain.entity.BoardPost;
import com.fairwaygms.fairwaygmsbe.board.domain.enums.AuthorType;
import com.fairwaygms.fairwaygmsbe.board.domain.repository.BoardCommentRepository;
import com.fairwaygms.fairwaygmsbe.board.domain.repository.BoardPostRepository;
import com.fairwaygms.fairwaygmsbe.board.exception.BoardErrorCode;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardCommentService {

    private final BoardCommentRepository boardCommentRepository;
    private final BoardPostRepository boardPostRepository;

    @Transactional
    public BoardCommentRes createComment(Long postId, CreateCommentReq req, AuthenticatedUser auth) {
        BoardPost post = getActivePost(postId);
        if (!post.getGolfCourseId().equals(auth.getGolfCourseId())) {
            throw new BusinessException(BoardErrorCode.COMMENT_ACCESS_DENIED);
        }
        AuthorType authorType = auth.isCaddy() ? AuthorType.CADDY : AuthorType.MANAGER;
        BoardComment comment = boardCommentRepository.save(
                BoardComment.create(postId, auth.getGolfCourseId(), auth.getUserId(), authorType, req.content()));
        return BoardCommentRes.from(comment);
    }

    @Transactional(readOnly = true)
    public Page<BoardCommentRes> getComments(Long postId, int page, int size, AuthenticatedUser auth) {
        BoardPost post = getActivePost(postId);
        if (!post.getGolfCourseId().equals(auth.getGolfCourseId())) {
            throw new BusinessException(BoardErrorCode.COMMENT_ACCESS_DENIED);
        }
        return boardCommentRepository
                .findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId, PageRequest.of(page, size))
                .map(BoardCommentRes::from);
    }

    @Transactional
    public void deleteComment(Long commentId, AuthenticatedUser auth) {
        BoardComment comment = boardCommentRepository.findById(commentId)
                .filter(c -> !c.getIsDeleted())
                .orElseThrow(() -> new BusinessException(BoardErrorCode.COMMENT_NOT_FOUND));
        if (!comment.getAuthorUserId().equals(auth.getUserId())) {
            throw new BusinessException(BoardErrorCode.COMMENT_ACCESS_DENIED);
        }
        comment.delete();
    }

    private BoardPost getActivePost(Long postId) {
        return boardPostRepository.findById(postId)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new BusinessException(BoardErrorCode.POST_NOT_FOUND));
    }
}
