package com.fairwaygms.fairwaygmsbe.board.integration;

import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.repository.UserRepository;
import com.fairwaygms.fairwaygmsbe.board.application.model.req.CreateCommentReq;
import com.fairwaygms.fairwaygmsbe.board.application.model.req.CreatePostReq;
import com.fairwaygms.fairwaygmsbe.board.application.model.req.CreateSwapRequestReq;
import com.fairwaygms.fairwaygmsbe.board.application.model.req.RejectSwapRequestReq;
import com.fairwaygms.fairwaygmsbe.board.application.model.req.UpdatePostReq;
import com.fairwaygms.fairwaygmsbe.board.application.model.res.BoardCommentRes;
import com.fairwaygms.fairwaygmsbe.board.application.model.res.BoardPostRes;
import com.fairwaygms.fairwaygmsbe.board.application.model.res.BoardPostSummaryRes;
import com.fairwaygms.fairwaygmsbe.board.application.model.res.SwapRequestRes;
import com.fairwaygms.fairwaygmsbe.board.application.service.BoardCommentService;
import com.fairwaygms.fairwaygmsbe.board.application.service.BoardPostService;
import com.fairwaygms.fairwaygmsbe.board.application.service.SwapRequestService;
import com.fairwaygms.fairwaygmsbe.board.domain.entity.BoardPost;
import com.fairwaygms.fairwaygmsbe.board.domain.enums.PostCategory;
import com.fairwaygms.fairwaygmsbe.board.domain.enums.SwapRequestStatus;
import com.fairwaygms.fairwaygmsbe.board.domain.repository.BoardPostRepository;
import com.fairwaygms.fairwaygmsbe.board.exception.BoardErrorCode;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieRepository;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.GolfCourseRepository;
import com.fairwaygms.fairwaygmsbe.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class BoardFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired BoardPostService boardPostService;
    @Autowired BoardCommentService boardCommentService;
    @Autowired SwapRequestService swapRequestService;
    @Autowired BoardPostRepository boardPostRepository;
    @Autowired GolfCourseRepository golfCourseRepository;
    @Autowired UserRepository userRepository;
    @Autowired CaddieRepository caddieRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private GolfCourse golfCourse;
    private AuthenticatedUser managerAuth;
    private AuthenticatedUser caddyAuth;
    private AuthenticatedUser caddyAuth2;
    private Caddie caddie;
    private Caddie caddie2;

    @BeforeEach
    void setUp() {
        golfCourse = golfCourseRepository.save(GolfCourse.create("테스트CC", "서울시", "02-0000-0000"));

        User managerUser = User.createEmailUser(
                "mgr@test.com", passwordEncoder.encode("Manager1!"), "매니저",
                null, UserRole.MANAGER, golfCourse.getId());
        managerUser.approve(999L);
        managerUser = userRepository.save(managerUser);
        managerAuth = new AuthenticatedUser(managerUser.getId(), UserRole.MANAGER, golfCourse.getId());

        User caddieUser1 = User.createEmailUser(
                "c1@test.com", passwordEncoder.encode("Caddie1!"), "캐디1",
                null, UserRole.CADDY, golfCourse.getId());
        caddieUser1.approve(999L);
        caddieUser1 = userRepository.save(caddieUser1);
        Caddie c1 = Caddie.createOnApproval(golfCourse, caddieUser1, "홍길동");
        c1.updateInfo("C001", null, null);
        caddie = caddieRepository.save(c1);
        caddyAuth = new AuthenticatedUser(caddieUser1.getId(), UserRole.CADDY, golfCourse.getId());

        User caddieUser2 = User.createEmailUser(
                "c2@test.com", passwordEncoder.encode("Caddie1!"), "캐디2",
                null, UserRole.CADDY, golfCourse.getId());
        caddieUser2.approve(999L);
        caddieUser2 = userRepository.save(caddieUser2);
        Caddie c2 = Caddie.createOnApproval(golfCourse, caddieUser2, "김철수");
        c2.updateInfo("C002", null, null);
        caddie2 = caddieRepository.save(c2);
        caddyAuth2 = new AuthenticatedUser(caddieUser2.getId(), UserRole.CADDY, golfCourse.getId());
    }

    // ─── 게시글 ───────────────────────────────────────────────────────────────

    @Test
    void 게시글_작성_조회_수정_삭제() {
        // 작성
        CreatePostReq req = new CreatePostReq(PostCategory.GENERAL_NOTICE, "공지제목", "공지내용");
        BoardPostRes created = boardPostService.createPost(req, managerAuth);
        assertThat(created.postId()).isNotNull();
        assertThat(created.title()).isEqualTo("공지제목");

        // 단건 조회
        BoardPostRes fetched = boardPostService.getPost(created.postId(), managerAuth);
        assertThat(fetched.content()).isEqualTo("공지내용");

        // 수정
        BoardPostRes updated = boardPostService.updatePost(
                created.postId(), new UpdatePostReq("수정제목", "수정내용"), managerAuth);
        assertThat(updated.title()).isEqualTo("수정제목");

        // 삭제
        boardPostService.deletePost(created.postId(), managerAuth);
        assertThatThrownBy(() -> boardPostService.getPost(created.postId(), managerAuth))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(BoardErrorCode.POST_NOT_FOUND.getMessage());
    }

    @Test
    void 게시글_목록_조회_댓글수_포함() {
        // given — 게시글 2건
        BoardPostRes post1 = boardPostService.createPost(
                new CreatePostReq(PostCategory.GENERAL_NOTICE, "공지1", "내용1"), managerAuth);
        BoardPostRes post2 = boardPostService.createPost(
                new CreatePostReq(PostCategory.SCHEDULE_NOTICE, "일정1", "내용2"), managerAuth);

        // 댓글 1건 추가
        boardCommentService.createComment(post1.postId(),
                new CreateCommentReq("댓글내용"), managerAuth);

        // when
        Page<BoardPostSummaryRes> result = boardPostService.getPosts(null, 0, 20, managerAuth);

        // then
        assertThat(result.getTotalElements()).isEqualTo(2);
        result.getContent().stream()
                .filter(p -> p.postId().equals(post1.postId()))
                .findFirst()
                .ifPresent(p -> assertThat(p.commentCount()).isEqualTo(1L));
    }

    // ─── 댓글 ─────────────────────────────────────────────────────────────────

    @Test
    void 댓글_작성_조회_삭제() {
        BoardPostRes post = boardPostService.createPost(
                new CreatePostReq(PostCategory.GENERAL_NOTICE, "제목", "내용"), managerAuth);

        // 댓글 작성
        BoardCommentRes comment = boardCommentService.createComment(
                post.postId(), new CreateCommentReq("댓글내용"), caddyAuth);
        assertThat(comment.commentId()).isNotNull();
        assertThat(comment.authorType()).isEqualTo("CADDY");

        // 목록 조회
        Page<BoardCommentRes> comments = boardCommentService.getComments(post.postId(), 0, 20, managerAuth);
        assertThat(comments.getTotalElements()).isEqualTo(1);

        // 삭제
        boardCommentService.deleteComment(comment.commentId(), caddyAuth);
        Page<BoardCommentRes> afterDelete = boardCommentService.getComments(post.postId(), 0, 20, managerAuth);
        assertThat(afterDelete.getTotalElements()).isEqualTo(0);
    }

    // ─── 교환 요청 ────────────────────────────────────────────────────────────

    @Test
    void 교환_요청_생성_성공() {
        CreateSwapRequestReq req = new CreateSwapRequestReq(caddie2.getId(), LocalDate.now(), "사유");
        SwapRequestRes result = swapRequestService.createSwapRequest(req, caddyAuth);

        assertThat(result.requestId()).isNotNull();
        assertThat(result.status()).isEqualTo(SwapRequestStatus.REQUESTED.name());
    }

    @Test
    void 자기_자신과_교환_요청_불가() {
        CreateSwapRequestReq req = new CreateSwapRequestReq(caddie.getId(), LocalDate.now(), "사유");
        assertThatThrownBy(() -> swapRequestService.createSwapRequest(req, caddyAuth))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(BoardErrorCode.SELF_SWAP_NOT_ALLOWED.getMessage());
    }

    @Test
    void 교환_요청_거절() {
        CreateSwapRequestReq req = new CreateSwapRequestReq(caddie2.getId(), LocalDate.now(), "사유");
        SwapRequestRes created = swapRequestService.createSwapRequest(req, caddyAuth);

        swapRequestService.rejectSwapRequest(
                created.requestId(), new RejectSwapRequestReq("거절 사유"), managerAuth);

        Page<SwapRequestRes> myRequests = swapRequestService.getMySwapRequests(0, 20, caddyAuth);
        assertThat(myRequests.getContent().get(0).status()).isEqualTo(SwapRequestStatus.REJECTED.name());
        assertThat(myRequests.getContent().get(0).rejectReason()).isEqualTo("거절 사유");
    }

    @Test
    void 이미_처리된_요청은_재처리_불가() {
        CreateSwapRequestReq req = new CreateSwapRequestReq(caddie2.getId(), LocalDate.now(), "사유");
        SwapRequestRes created = swapRequestService.createSwapRequest(req, caddyAuth);

        swapRequestService.rejectSwapRequest(
                created.requestId(), new RejectSwapRequestReq("거절"), managerAuth);

        assertThatThrownBy(() -> swapRequestService.rejectSwapRequest(
                created.requestId(), new RejectSwapRequestReq("재거절"), managerAuth))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(BoardErrorCode.SWAP_REQUEST_ALREADY_PROCESSED.getMessage());
    }
}
