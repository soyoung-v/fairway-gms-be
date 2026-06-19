package com.fairwaygms.fairwaygmsbe.golfcourse.application.controller;

import com.fairwaygms.fairwaygmsbe.common.response.ApiResponse;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.request.CreateGolfCourseRequest;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.request.UpdateGolfCourseRequest;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.response.GolfCourseResponse;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.service.GolfCourseService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 골프장 등록/수정/조회/선택 API
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/golf-course/golf-courses")
public class GolfCourseController {

    private final GolfCourseService golfCourseService;

    // API-201: 골프장 등록 (Admin)
    @PostMapping
    public ResponseEntity<ApiResponse<GolfCourseResponse>> createGolfCourse(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateGolfCourseRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(golfCourseService.createGolfCourse(request, user)));
    }

    // API-202: 골프장 수정 (Admin)
    @PatchMapping("/{golfCourseId}")
    public ResponseEntity<ApiResponse<GolfCourseResponse>> updateGolfCourse(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long golfCourseId,
            @Valid @RequestBody UpdateGolfCourseRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                golfCourseService.updateGolfCourse(golfCourseId, request, user)));
    }

    // API-203: 골프장 목록 조회 (Admin: 전체, Manager: 본인 소속만)
    @GetMapping
    public ResponseEntity<ApiResponse<List<GolfCourseResponse>>> listGolfCourses(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(ApiResponse.success(golfCourseService.listGolfCourses(user)));
    }

    // API-204: 골프장 선택 — 존재/상태 검증 후 기본 정보 반환. Manager는 본인 골프장만 선택 가능.
    @PostMapping("/{golfCourseId}/select")
    public ResponseEntity<ApiResponse<GolfCourseResponse>> selectGolfCourse(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long golfCourseId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                golfCourseService.selectGolfCourse(golfCourseId, user)));
    }
}
