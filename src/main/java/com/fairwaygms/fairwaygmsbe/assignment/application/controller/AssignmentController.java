package com.fairwaygms.fairwaygmsbe.assignment.application.controller;

import com.fairwaygms.fairwaygmsbe.assignment.application.model.req.*;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.AssignmentHistoryRes;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.AssignmentRes;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.AutoAssignRes;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.CourseAssignmentRes;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.UnassignedTeamRes;
import com.fairwaygms.fairwaygmsbe.assignment.application.service.AssignmentExcelService;
import com.fairwaygms.fairwaygmsbe.assignment.application.service.AssignmentService;
import com.fairwaygms.fairwaygmsbe.common.response.ApiResponse;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.config.AdminScopeApi;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@AdminScopeApi
@Tag(name = "캐디 배정")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/assignment")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final AssignmentExcelService assignmentExcelService;

    // API-516: 배정표 엑셀 다운로드 (FR-523)
    @GetMapping("/excel")
    public ResponseEntity<byte[]> downloadScheduleExcel(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate assignmentDate
    ) {
        byte[] file = assignmentExcelService.exportDailySchedule(assignmentDate, auth);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("assignment_" + assignmentDate + ".xlsx", StandardCharsets.UTF_8)
                .build());

        return ResponseEntity.ok().headers(headers).body(file);
    }

    // API-512: 코스별 배정표 조회 (FR-519)
    @GetMapping("/schedules/by-course")
    public ResponseEntity<ApiResponse<List<CourseAssignmentRes>>> getAssignmentsByCourse(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @RequestParam(required = false) Long golfCourseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate assignmentDate,
            @RequestParam Long courseId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                assignmentService.getAssignmentsByCourse(golfCourseId, assignmentDate, courseId, auth)));
    }

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

    // API-501: 자동배정 실행 (FR-501~505)
    @PostMapping("/auto")
    public ResponseEntity<ApiResponse<AutoAssignRes>> autoAssign(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @Valid @RequestBody AutoAssignReq request
    ) {
        return ResponseEntity.ok(ApiResponse.success(assignmentService.autoAssign(request, auth)));
    }

    // API-506: 배정 교환 (FR-511) — 이미 배정된 두 캐디를 맞바꿈
    @PostMapping("/swap")
    public ResponseEntity<ApiResponse<Void>> swapAssignments(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @Valid @RequestBody SwapAssignmentReq request
    ) {
        assignmentService.swapAssignments(request, auth);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // API-510: 미배정 팀 조회 (FR-517)
    @GetMapping("/unassigned")
    public ResponseEntity<ApiResponse<List<UnassignedTeamRes>>> getUnassignedTeams(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @RequestParam(required = false) Long golfCourseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate assignmentDate
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                assignmentService.getUnassignedTeams(golfCourseId, assignmentDate, auth)));
    }

    // API-515: 단건 배정 완료 처리 (FR-522)
    @PatchMapping("/{assignmentId}/complete")
    public ResponseEntity<ApiResponse<AssignmentRes>> completeAssignment(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long assignmentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                assignmentService.completeAssignment(assignmentId, auth)));
    }
}
