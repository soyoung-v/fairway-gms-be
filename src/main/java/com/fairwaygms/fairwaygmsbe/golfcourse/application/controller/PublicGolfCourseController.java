package com.fairwaygms.fairwaygmsbe.golfcourse.application.controller;

import com.fairwaygms.fairwaygmsbe.common.response.ApiResponse;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.res.PublicGolfCourseRes;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.service.GolfCourseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 비로그인 공개 API — 회원가입 화면의 골프장 선택 드롭다운용
@Tag(name = "공개 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public")
public class PublicGolfCourseController {

    private final GolfCourseService golfCourseService;

    @GetMapping("/golf-courses")
    public ResponseEntity<ApiResponse<List<PublicGolfCourseRes>>> getPublicGolfCourses() {
        return ResponseEntity.ok(ApiResponse.success(golfCourseService.getPublicList()));
    }
}
