package com.fairwaygms.fairwaygmsbe.assignment.application.controller;

import com.fairwaygms.fairwaygmsbe.assignment.application.model.req.CancelAssignmentReq;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.req.ManualPreAssignReq;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.req.ReassignReq;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.req.UnlockAssignmentReq;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.AssignmentHistoryRes;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.AssignmentRes;
import com.fairwaygms.fairwaygmsbe.assignment.application.service.AssignmentService;
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
@RequestMapping("/api/assignment")
public class AssignmentController {

    private final AssignmentService assignmentService;

    // API-511: 일일 배정표 조회 (FR-518)
    @GetMapping("/schedules/daily")
    public ResponseEntity<ApiResponse<List<AssignmentRes>>> getDailyAssignments(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @RequestParam(required = false) Long golfCourseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate assignmentDate
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                assignmentService.getAssignments(golfCourseId, assignmentDate, auth)));
    }

    // API-503: 수동 배정 (FR-508)
    @PostMapping("/manual")
    public ResponseEntity<ApiResponse<AssignmentRes>> manualPreAssign(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @Valid @RequestBody ManualPreAssignReq request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(assignmentService.manualPreAssign(request, auth)));
    }

    // API-504: 재배정 (FR-509)
    @PatchMapping("/{assignmentId}/caddie")
    public ResponseEntity<ApiResponse<AssignmentRes>> reassign(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long assignmentId,
            @Valid @RequestBody ReassignReq request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                assignmentService.reassign(assignmentId, request, auth)));
    }

    // API-505: 배정 취소 (FR-510)
    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<ApiResponse<Void>> cancelAssignment(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long assignmentId,
            @RequestBody(required = false) CancelAssignmentReq request
    ) {
        String reason = request != null ? request.reason() : null;
        assignmentService.cancelAssignment(assignmentId, reason, auth);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // API-507: 지정 캐디 잠금 강제 해제 (FR-512)
    @PatchMapping("/{assignmentId}/lock/release")
    public ResponseEntity<ApiResponse<AssignmentRes>> unlock(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long assignmentId,
            @Valid @RequestBody UnlockAssignmentReq request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                assignmentService.unlock(assignmentId, request, auth)));
    }

    // API-517: 배정 변경 이력 조회 (FR-524)
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<AssignmentHistoryRes>>> getHistory(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @RequestParam(required = false) Long golfCourseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate assignmentDate,
            @RequestParam(required = false) Long caddieId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                assignmentService.getHistory(golfCourseId, assignmentDate, caddieId, auth)));
    }
}
