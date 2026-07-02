package com.fairwaygms.fairwaygmsbe.auth.application.controller;

import com.fairwaygms.fairwaygmsbe.auth.application.model.res.AdminUserRes;
import com.fairwaygms.fairwaygmsbe.auth.application.service.AdminUserService;
import com.fairwaygms.fairwaygmsbe.common.response.ApiResponse;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "사용자 관리")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/users")
public class ManagerUserController {

    private final AdminUserService adminUserService;

    // Manager 전용 — 자기 골프장 소속 CADDY 승인 대기 목록
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<AdminUserRes>>> pendingCaddies(
            @AuthenticationPrincipal AuthenticatedUser manager
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.getPendingCaddiesForManager(manager)));
    }

    // Manager 전용 — 자기 골프장 소속 CADDY 승인
    @PatchMapping("/{userId}/approve-caddie")
    public ResponseEntity<ApiResponse<AdminUserRes>> approveCaddie(
            @AuthenticationPrincipal AuthenticatedUser manager,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.approveCaddieAsManager(manager, userId)));
    }

    // Manager 전용 — 자기 골프장 소속 CADDY 거절
    @PatchMapping("/{userId}/reject-caddie")
    public ResponseEntity<ApiResponse<AdminUserRes>> rejectCaddie(
            @AuthenticationPrincipal AuthenticatedUser manager,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.rejectCaddieAsManager(manager, userId)));
    }
}
