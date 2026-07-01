package com.fairwaygms.fairwaygmsbe.assignment.application.controller;

import com.fairwaygms.fairwaygmsbe.assignment.application.model.req.CreateDailyScheduleReq;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.DailyScheduleRes;
import com.fairwaygms.fairwaygmsbe.assignment.application.service.DailyScheduleService;
import com.fairwaygms.fairwaygmsbe.common.response.ApiResponse;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/assignment/schedules")
public class DailyScheduleController {

    private final DailyScheduleService dailyScheduleService;

    // 배정표 생성 (DRAFT)
    @PostMapping
    public ResponseEntity<ApiResponse<DailyScheduleRes>> createDailySchedule(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @Valid @RequestBody CreateDailyScheduleReq request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dailyScheduleService.createDailySchedule(request, auth)));
    }

    // 배정표 단건 조회
    @GetMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<DailyScheduleRes>> getDailySchedule(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long scheduleId
    ) {
        return ResponseEntity.ok(ApiResponse.success(dailyScheduleService.getDailySchedule(scheduleId, auth)));
    }

    // API-513: 배정 확정 (FR-520)
    @PostMapping("/{scheduleId}/confirm")
    public ResponseEntity<ApiResponse<DailyScheduleRes>> confirmSchedule(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long scheduleId
    ) {
        return ResponseEntity.ok(ApiResponse.success(dailyScheduleService.confirmSchedule(scheduleId, auth)));
    }

    // API-514: 배정 확정 취소 (FR-521)
    @DeleteMapping("/{scheduleId}/confirm")
    public ResponseEntity<ApiResponse<DailyScheduleRes>> cancelConfirmSchedule(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long scheduleId
    ) {
        return ResponseEntity.ok(ApiResponse.success(dailyScheduleService.cancelConfirmSchedule(scheduleId, auth)));
    }

    // 배정표 완료 처리 → AssignmentCompletedEvent 발행
    @PostMapping("/{scheduleId}/complete")
    public ResponseEntity<ApiResponse<DailyScheduleRes>> completeDailySchedule(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long scheduleId
    ) {
        return ResponseEntity.ok(ApiResponse.success(dailyScheduleService.completeDailySchedule(scheduleId, auth)));
    }
}
