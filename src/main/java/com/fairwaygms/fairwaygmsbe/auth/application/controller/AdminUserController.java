package com.fairwaygms.fairwaygmsbe.auth.application.controller;

import com.fairwaygms.fairwaygmsbe.auth.application.model.response.AdminUserResponse;
import com.fairwaygms.fairwaygmsbe.auth.application.service.AdminUserService;
import com.fairwaygms.fairwaygmsbe.common.response.ApiResponse;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    // ADMIN 전용 승인 대기 계정 목록
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> pendingUsers(
            @AuthenticationPrincipal AuthenticatedUser admin
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.getPendingUsers(admin)));
    }

    // ADMIN 전용 사용자 승인
    @PatchMapping("/{userId}/approve")
    public ResponseEntity<ApiResponse<AdminUserResponse>> approveUser(
            @AuthenticationPrincipal AuthenticatedUser admin,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.approveUser(admin, userId)));
    }

    // ADMIN 전용 사용자 거절
    @PatchMapping("/{userId}/reject")
    public ResponseEntity<ApiResponse<AdminUserResponse>> rejectUser(
            @AuthenticationPrincipal AuthenticatedUser admin,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.rejectUser(admin, userId)));
    }

    // ADMIN 전용 Manager 퇴사 처리
    @PatchMapping("/{userId}/withdraw")
    public ResponseEntity<ApiResponse<AdminUserResponse>> withdrawManager(
            @AuthenticationPrincipal AuthenticatedUser admin,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.withdrawManager(admin, userId)));
    }
}
