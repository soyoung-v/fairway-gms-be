package com.fairwaygms.fairwaygmsbe.board.application.controller;

import com.fairwaygms.fairwaygmsbe.board.application.model.req.CreateCommentReq;
import com.fairwaygms.fairwaygmsbe.board.application.model.req.CreatePostReq;
import com.fairwaygms.fairwaygmsbe.board.application.model.req.CreateSwapRequestReq;
import com.fairwaygms.fairwaygmsbe.board.application.model.req.RejectSwapRequestReq;
import com.fairwaygms.fairwaygmsbe.board.application.model.req.UpdatePostReq;
import com.fairwaygms.fairwaygmsbe.board.application.model.res.BoardCommentRes;
import com.fairwaygms.fairwaygmsbe.board.application.model.res.BoardPostRes;
import com.fairwaygms.fairwaygmsbe.board.application.model.res.BoardPostSummaryRes;
import com.fairwaygms.fairwaygmsbe.board.application.model.res.SwapRequestRes;
import com.fairwaygms.fairwaygmsbe.board.application.model.res.SwapRequestSummaryRes;
import com.fairwaygms.fairwaygmsbe.board.application.service.BoardCommentService;
import com.fairwaygms.fairwaygmsbe.board.application.service.BoardPostService;
import com.fairwaygms.fairwaygmsbe.board.application.service.SwapRequestService;
import com.fairwaygms.fairwaygmsbe.board.domain.enums.PostCategory;
import com.fairwaygms.fairwaygmsbe.common.config.AdminScopeApi;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.fairwaygms.fairwaygmsbe.board.domain.enums.SwapRequestStatus;
import com.fairwaygms.fairwaygmsbe.common.response.ApiResponse;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AdminScopeApi
@Tag(name = "게시판")
@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardPostService boardPostService;
    private final BoardCommentService boardCommentService;
    private final SwapRequestService swapRequestService;

    // ─── 게시글 ────────────────────────────────────────────────────────────────

    // API-801: 게시글 목록 조회
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<Page<BoardPostSummaryRes>>> getPosts(
            @RequestParam(required = false) PostCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return ResponseEntity.ok(ApiResponse.success(boardPostService.getPosts(category, page, size, auth)));
    }

    // API-802: 게시글 단건 조회
    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<BoardPostRes>> getPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return ResponseEntity.ok(ApiResponse.success(boardPostService.getPost(postId, auth)));
    }

    // API-803: 게시글 작성
    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<BoardPostRes>> createPost(
            @RequestBody @Valid CreatePostReq req,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return ResponseEntity.ok(ApiResponse.success(boardPostService.createPost(req, auth)));
    }

    // API-804: 게시글 수정
    @PutMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<BoardPostRes>> updatePost(
            @PathVariable Long postId,
            @RequestBody @Valid UpdatePostReq req,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return ResponseEntity.ok(ApiResponse.success(boardPostService.updatePost(postId, req, auth)));
    }

    // API-805: 게시글 삭제
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        boardPostService.deletePost(postId, auth);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ─── 댓글 ─────────────────────────────────────────────────────────────────

    // API-806: 댓글 목록 조회
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<Page<BoardCommentRes>>> getComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return ResponseEntity.ok(ApiResponse.success(boardCommentService.getComments(postId, page, size, auth)));
    }

    // API-807: 댓글 작성
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<BoardCommentRes>> createComment(
            @PathVariable Long postId,
            @RequestBody @Valid CreateCommentReq req,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return ResponseEntity.ok(ApiResponse.success(boardCommentService.createComment(postId, req, auth)));
    }

    // API-808: 댓글 삭제
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        boardCommentService.deleteComment(commentId, auth);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ─── 교환 요청 ─────────────────────────────────────────────────────────────

    // API-809: 교환 요청 목록 조회 (매니저)
    @GetMapping("/swap-requests")
    public ResponseEntity<ApiResponse<Page<SwapRequestSummaryRes>>> getSwapRequests(
            @RequestParam(required = false) SwapRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return ResponseEntity.ok(ApiResponse.success(swapRequestService.getSwapRequests(status, page, size, auth)));
    }

    // API-810: 교환 요청 생성 (캐디)
    @PostMapping("/swap-requests")
    public ResponseEntity<ApiResponse<SwapRequestRes>> createSwapRequest(
            @RequestBody @Valid CreateSwapRequestReq req,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return ResponseEntity.ok(ApiResponse.success(swapRequestService.createSwapRequest(req, auth)));
    }

    // API-811: 교환 요청 승인 (매니저)
    @PatchMapping("/swap-requests/{requestId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveSwapRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        swapRequestService.approveSwapRequest(requestId, auth);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // API-812: 교환 요청 거절 (매니저)
    @PatchMapping("/swap-requests/{requestId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectSwapRequest(
            @PathVariable Long requestId,
            @RequestBody @Valid RejectSwapRequestReq req,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        swapRequestService.rejectSwapRequest(requestId, req, auth);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // API-813: 내 교환 요청 목록 조회 (캐디)
    @GetMapping("/swap-requests/my")
    public ResponseEntity<ApiResponse<Page<SwapRequestRes>>> getMySwapRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return ResponseEntity.ok(ApiResponse.success(swapRequestService.getMySwapRequests(page, size, auth)));
    }
}
