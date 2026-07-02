package com.fairwaygms.fairwaygmsbe.golfcourse.application.controller;

import com.fairwaygms.fairwaygmsbe.common.response.ApiResponse;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.req.CreateCartReq;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.req.UpdateCartReq;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.req.UpdateCartStatusReq;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.res.CartRes;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.service.GolfCourseService;
import com.fairwaygms.fairwaygmsbe.common.config.AdminScopeApi;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 카트 등록/수정/조회/상태변경/반납 API
@AdminScopeApi
@Tag(name = "카트 관리")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/golf-course")
public class CartController {

    private final GolfCourseService golfCourseService;

    // API-208: 카트 등록 (Admin, Manager)
    @PostMapping("/golf-courses/{golfCourseId}/carts")
    public ResponseEntity<ApiResponse<CartRes>> createCart(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long golfCourseId,
            @Valid @RequestBody CreateCartReq request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(golfCourseService.createCart(golfCourseId, request, user)));
    }

    // API-209: 카트 목록 조회 (Admin, Manager) — status 파라미터로 필터링 가능
    @GetMapping("/golf-courses/{golfCourseId}/carts")
    public ResponseEntity<ApiResponse<List<CartRes>>> listCarts(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long golfCourseId,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                golfCourseService.listCarts(golfCourseId, status, user)));
    }

    // API-210: 카트 수정 (Admin, Manager)
    @PatchMapping("/carts/{cartId}")
    public ResponseEntity<ApiResponse<CartRes>> updateCart(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long cartId,
            @Valid @RequestBody UpdateCartReq request
    ) {
        return ResponseEntity.ok(ApiResponse.success(golfCourseService.updateCart(cartId, request, user)));
    }

    // API-211: 카트 상태 변경 (Manager)
    @PatchMapping("/carts/{cartId}/status")
    public ResponseEntity<ApiResponse<CartRes>> updateCartStatus(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long cartId,
            @Valid @RequestBody UpdateCartStatusReq request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                golfCourseService.updateCartStatus(cartId, request, user)));
    }

    // API-212: 카트 반납 처리 — Assignment 도메인의 AssignmentCompletedEvent 연동 예정
    @PatchMapping("/carts/{cartId}/return")
    public ResponseEntity<ApiResponse<Void>> returnCart(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long cartId
    ) {
        golfCourseService.returnCart(cartId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
