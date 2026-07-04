package com.fairwaygms.fairwaygmsbe.assignment.application.controller;

import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.MyAssignmentDetailRes;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.MyAssignmentRes;
import com.fairwaygms.fairwaygmsbe.assignment.application.service.AssignmentMobileService;
import com.fairwaygms.fairwaygmsbe.common.response.ApiResponse;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "캐디 모바일")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/assignment/me")
public class AssignmentMobileController {

    private final AssignmentMobileService assignmentMobileService;

    // API-518: 내 배정 조회 (FR-526) — targetDate 생략 시 오늘 기준
    @GetMapping
    public ResponseEntity<ApiResponse<List<MyAssignmentRes>>> getMyAssignments(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                assignmentMobileService.getMyAssignments(targetDate, auth)));
    }

    // API-519: 내 티타임 상세 조회 (FR-527)
    @GetMapping("/{assignmentId}")
    public ResponseEntity<ApiResponse<MyAssignmentDetailRes>> getMyAssignmentDetail(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long assignmentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                assignmentMobileService.getMyAssignmentDetail(assignmentId, auth)));
    }
}
