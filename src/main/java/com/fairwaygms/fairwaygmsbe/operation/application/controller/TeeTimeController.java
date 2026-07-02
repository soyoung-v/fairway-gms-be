package com.fairwaygms.fairwaygmsbe.operation.application.controller;

import com.fairwaygms.fairwaygmsbe.common.response.ApiResponse;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.BulkRegenerateTeeTimesReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.CreateTeeTimeReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.GenerateTeeTimesReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.res.GenerateTeeTimesRes;
import com.fairwaygms.fairwaygmsbe.operation.application.model.res.TeeTimeRes;
import com.fairwaygms.fairwaygmsbe.operation.application.service.TeeTimeService;
import com.fairwaygms.fairwaygmsbe.common.config.AdminScopeApi;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@AdminScopeApi
@Tag(name = "티타임")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/operation/tee-times")
public class TeeTimeController {

    private final TeeTimeService teeTimeService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<GenerateTeeTimesRes>> generateTeeTimes(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @Valid @RequestBody GenerateTeeTimesReq request
    ) {
        return ResponseEntity.ok(ApiResponse.success(teeTimeService.generateTeeTimes(request, auth)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TeeTimeRes>>> listTeeTimes(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate playDate,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Integer periodNumber
    ) {
        return ResponseEntity.ok(ApiResponse.success(teeTimeService.listTeeTimes(playDate, courseId, periodNumber, auth)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TeeTimeRes>> addTeeTime(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @Valid @RequestBody CreateTeeTimeReq request
    ) {
        return ResponseEntity.ok(ApiResponse.success(teeTimeService.addTeeTime(request, auth)));
    }

    @PatchMapping("/{teeTimeId}/close")
    public ResponseEntity<ApiResponse<Void>> closeTeeTime(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long teeTimeId
    ) {
        teeTimeService.closeTeeTime(teeTimeId, auth);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/bulk-regenerate")
    public ResponseEntity<ApiResponse<GenerateTeeTimesRes>> bulkRegenerate(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @Valid @RequestBody BulkRegenerateTeeTimesReq request
    ) {
        return ResponseEntity.ok(ApiResponse.success(teeTimeService.bulkRegenerate(request, auth)));
    }
}
