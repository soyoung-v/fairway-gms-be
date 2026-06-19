package com.fairwaygms.fairwaygmsbe.golfcourse.application.controller;

import com.fairwaygms.fairwaygmsbe.common.response.ApiResponse;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.request.CreateCourseRequest;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.request.UpdateCourseRequest;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.response.CourseResponse;
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

// 코스 등록/수정/조회 API
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/golf-course")
public class CourseController {

    private final GolfCourseService golfCourseService;

    // API-205: 코스 등록 (Admin, Manager)
    @PostMapping("/golf-courses/{golfCourseId}/courses")
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long golfCourseId,
            @Valid @RequestBody CreateCourseRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(golfCourseService.createCourse(golfCourseId, request, user)));
    }

    // API-206: 코스 수정 (Admin, Manager)
    @PatchMapping("/courses/{courseId}")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long courseId,
            @Valid @RequestBody UpdateCourseRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(golfCourseService.updateCourse(courseId, request, user)));
    }

    // API-207: 코스 목록 조회 (Admin, Manager) — sortOrder 오름차순
    @GetMapping("/golf-courses/{golfCourseId}/courses")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> listCourses(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long golfCourseId
    ) {
        return ResponseEntity.ok(ApiResponse.success(golfCourseService.listCourses(golfCourseId, user)));
    }
}
