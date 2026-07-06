package com.fairwaygms.fairwaygmsbe.caddie.application.controller;

import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.CreateCaddieGroupReq;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.UpdateCaddieGroupReq;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.CaddieGroupRes;
import com.fairwaygms.fairwaygmsbe.caddie.application.service.CaddieGroupService;
import com.fairwaygms.fairwaygmsbe.common.response.ApiResponse;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ADR-005: 자동배정 그룹(하우스/우선배정/세션고정) 관리 — MANAGER 전용
@Tag(name = "캐디 그룹")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/caddie/groups")
public class CaddieGroupController {

    private final CaddieGroupService caddieGroupService;

    // 그룹 목록 조회 — 최초 조회 시 기본 하우스 그룹 자동 생성
    @GetMapping
    public ResponseEntity<ApiResponse<List<CaddieGroupRes>>> getGroups(
            @AuthenticationPrincipal AuthenticatedUser auth
    ) {
        return ResponseEntity.ok(ApiResponse.success(caddieGroupService.getGroups(auth)));
    }

    // 그룹 등록 (예: 주말반=PRIORITY_FIRST, 주중2부반=SESSION_FIXED)
    @PostMapping
    public ResponseEntity<ApiResponse<CaddieGroupRes>> create(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @Valid @RequestBody CreateCaddieGroupReq request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(caddieGroupService.create(request, auth)));
    }

    // 그룹명/배정유형 수정
    @PatchMapping("/{groupId}")
    public ResponseEntity<ApiResponse<CaddieGroupRes>> update(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long groupId,
            @Valid @RequestBody UpdateCaddieGroupReq request
    ) {
        return ResponseEntity.ok(ApiResponse.success(caddieGroupService.update(groupId, request, auth)));
    }

    // 그룹 삭제 (소속 캐디가 있으면 409)
    @DeleteMapping("/{groupId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long groupId
    ) {
        caddieGroupService.delete(groupId, auth);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
