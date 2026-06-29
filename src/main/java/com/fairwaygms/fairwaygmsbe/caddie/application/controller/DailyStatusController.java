package com.fairwaygms.fairwaygmsbe.caddie.application.controller;

import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.RegisterDailyStatusReq;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.DailyStatusRes;
import com.fairwaygms.fairwaygmsbe.caddie.application.service.DailyStatusService;
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
@RequestMapping("/api/caddie/daily-status")
public class DailyStatusController {

    private final DailyStatusService dailyStatusService;

    // FR-317~323: 일별 근무 상태 등록 (휴무/결근/당번/조출/특수근무/배정제외)
    @PostMapping
    public ResponseEntity<ApiResponse<DailyStatusRes>> register(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @Valid @RequestBody RegisterDailyStatusReq request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dailyStatusService.register(request, auth)));
    }

    // 날짜 기준 일별 근무 상태 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<DailyStatusRes>>> getByDate(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @RequestParam(required = false) Long golfCourseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate statusDate
    ) {
        return ResponseEntity.ok(ApiResponse.success(dailyStatusService.getByDate(golfCourseId, statusDate, auth)));
    }

    // FR-316: 일별 근무 상태 삭제
    @DeleteMapping("/{statusId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long statusId
    ) {
        dailyStatusService.delete(statusId, auth);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
