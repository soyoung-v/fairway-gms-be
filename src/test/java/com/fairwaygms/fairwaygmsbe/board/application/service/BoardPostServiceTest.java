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
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoardPostServiceTest {

    @Mock private BoardPostRepository boardPostRepository;
    @Mock private BoardCommentRepository boardCommentRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private BoardPostService boardPostService;

    private AuthenticatedUser managerAuth;
    private AuthenticatedUser otherManagerAuth;

    @BeforeEach
    void setUp() {
        boardPostService = new BoardPostService(boardPostRepository, boardCommentRepository, eventPublisher);
        managerAuth = new AuthenticatedUser(10L, UserRole.MANAGER, 1L);
        otherManagerAuth = new AuthenticatedUser(20L, UserRole.MANAGER, 2L);
    }

    @Test
    void createPost_성공_이벤트_발행() {
        // given
        CreatePostReq req = new CreatePostReq(PostCategory.GENERAL_NOTICE, "제목", "내용");
        BoardPost post = BoardPost.create(1L, 10L, PostCategory.GENERAL_NOTICE, "제목", "내용");
        ReflectionTestUtils.setField(post, "id", 100L);

        when(boardPostRepository.save(any(BoardPost.class))).thenReturn(post);

        // when
        BoardPostRes result = boardPostService.createPost(req, managerAuth);

        // then
        assertThat(result.title()).isEqualTo("제목");
        ArgumentCaptor<BoardPostCreatedEvent> captor = ArgumentCaptor.forClass(BoardPostCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getPostId()).isEqualTo(100L);
        assertThat(captor.getValue().getGolfCourseId()).isEqualTo(1L);
    }

    @Test
    void updatePost_작성자가_아니면_ACCESS_DENIED() {
        // given
        BoardPost post = BoardPost.create(1L, 99L, PostCategory.GENERAL_NOTICE, "제목", "내용");
        ReflectionTestUtils.setField(post, "id", 100L);
        when(boardPostRepository.findById(100L)).thenReturn(Optional.of(post));

        UpdatePostReq req = new UpdatePostReq("수정 제목", "수정 내용");

        // when / then
        assertThatThrownBy(() -> boardPostService.updatePost(100L, req, managerAuth))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(BoardErrorCode.POST_ACCESS_DENIED.getMessage());
    }

    @Test
    void updatePost_성공() {
        // given
        BoardPost post = BoardPost.create(1L, 10L, PostCategory.GENERAL_NOTICE, "제목", "내용");
        ReflectionTestUtils.setField(post, "id", 100L);
        when(boardPostRepository.findById(100L)).thenReturn(Optional.of(post));

        UpdatePostReq req = new UpdatePostReq("수정 제목", "수정 내용");

        // when
        BoardPostRes result = boardPostService.updatePost(100L, req, managerAuth);

        // then
        assertThat(result.title()).isEqualTo("수정 제목");
        assertThat(result.content()).isEqualTo("수정 내용");
    }

    @Test
    void deletePost_다른_골프장이면_ACCESS_DENIED() {
        // given
        BoardPost post = BoardPost.create(1L, 10L, PostCategory.GENERAL_NOTICE, "제목", "내용");
        ReflectionTestUtils.setField(post, "id", 100L);
        when(boardPostRepository.findById(100L)).thenReturn(Optional.of(post));

        // when / then — otherManagerAuth 는 golfCourseId=2
        assertThatThrownBy(() -> boardPostService.deletePost(100L, otherManagerAuth))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deletePost_성공() {
        // given
        BoardPost post = BoardPost.create(1L, 10L, PostCategory.GENERAL_NOTICE, "제목", "내용");
        ReflectionTestUtils.setField(post, "id", 100L);
        when(boardPostRepository.findById(100L)).thenReturn(Optional.of(post));

        // when
        boardPostService.deletePost(100L, managerAuth);

        // then
        assertThat(post.getIsDeleted()).isTrue();
    }

    @Test
    void getPosts_카테고리_필터_포함_commentCount_조회() {
        // given
        BoardPost post = BoardPost.create(1L, 10L, PostCategory.GENERAL_NOTICE, "제목", "내용");
        ReflectionTestUtils.setField(post, "id", 100L);
        Page<BoardPost> pageResult = new PageImpl<>(List.of(post));

        when(boardPostRepository.findByGolfCourseAndCategory(eq(1L), isNull(), any()))
                .thenReturn(pageResult);
        when(boardCommentRepository.countByPostIdAndIsDeletedFalse(100L)).thenReturn(3L);

        // when
        Page<BoardPostSummaryRes> result = boardPostService.getPosts(null, 0, 20, managerAuth);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).commentCount()).isEqualTo(3L);
    }

    @Test
    void getPost_존재하지_않으면_POST_NOT_FOUND() {
        // given
        when(boardPostRepository.findById(999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> boardPostService.getPost(999L, managerAuth))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(BoardErrorCode.POST_NOT_FOUND.getMessage());
    }
}
