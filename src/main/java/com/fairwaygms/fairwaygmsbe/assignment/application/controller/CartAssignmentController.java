package com.fairwaygms.fairwaygmsbe.assignment.application.controller;

import com.fairwaygms.fairwaygmsbe.assignment.application.model.req.CartAssignReq;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.req.ChangeCartReq;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.CartAssignmentRes;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.CartAutoAssignRes;
import com.fairwaygms.fairwaygmsbe.assignment.application.service.CartAssignmentService;
import com.fairwaygms.fairwaygmsbe.common.response.ApiResponse;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/assignment/carts")
public class CartAssignmentController {

    private final CartAssignmentService cartAssignmentService;

    // 카트-티타임 수동 배정
    @PostMapping
    public ResponseEntity<ApiResponse<CartAssignmentRes>> assignCart(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @Valid @RequestBody CartAssignReq request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(cartAssignmentService.assignCart(request, auth)));
    }

    // API-520: 카트 자동 배정 (FR-530)
    @PostMapping("/auto")
    public ResponseEntity<ApiResponse<CartAutoAssignRes>> autoAssignCarts(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate assignmentDate
    ) {
        return ResponseEntity.ok(ApiResponse.success(cartAssignmentService.autoAssignCarts(assignmentDate, auth)));
    }

    // API-521: 카트 수동 변경 (FR-531)
    @PatchMapping("/{cartAssignmentId}")
    public ResponseEntity<ApiResponse<CartAssignmentRes>> changeCart(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long cartAssignmentId,
            @Valid @RequestBody ChangeCartReq request
    ) {
        return ResponseEntity.ok(ApiResponse.success(cartAssignmentService.changeCart(cartAssignmentId, request, auth)));
    }

    // 카트 반납 처리
    @PatchMapping("/{cartAssignmentId}/return")
    public ResponseEntity<ApiResponse<CartAssignmentRes>> returnCart(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long cartAssignmentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(cartAssignmentService.returnCart(cartAssignmentId, auth)));
    }

    // 카트 배정 취소
    @DeleteMapping("/{cartAssignmentId}")
    public ResponseEntity<ApiResponse<Void>> cancelCartAssignment(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long cartAssignmentId
    ) {
        cartAssignmentService.cancelCartAssignment(cartAssignmentId, auth);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 골프장+날짜 기준 카트 배정 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<CartAssignmentRes>>> getCartAssignments(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @RequestParam(required = false) Long golfCourseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate assignmentDate
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                cartAssignmentService.getCartAssignments(golfCourseId, assignmentDate, auth)));
    }
}
