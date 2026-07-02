package com.fairwaygms.fairwaygmsbe.caddie.application.controller;

import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.AdjustQueueReq;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.InitializeQueueReq;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.InitializeQueueRes;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.QueueRes;
import com.fairwaygms.fairwaygmsbe.caddie.application.service.QueueService;
import com.fairwaygms.fairwaygmsbe.common.response.ApiResponse;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
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
@Tag(name = "순번 관리")
@RestController
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    // FR-312: 날짜 기준 대기 순번 목록 조회
    @GetMapping("/api/caddie/queues")
    public ResponseEntity<ApiResponse<List<QueueRes>>> getQueues(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @RequestParam(required = false) Long golfCourseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate queueDate
    ) {
        return ResponseEntity.ok(ApiResponse.success(queueService.getQueues(golfCourseId, queueDate, auth)));
    }

    // FR-313: 대기 순번 초기화
    @PostMapping("/api/caddie/queues/initialize")
    public ResponseEntity<ApiResponse<InitializeQueueRes>> initializeQueues(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @Valid @RequestBody InitializeQueueReq request
    ) {
        return ResponseEntity.ok(ApiResponse.success(queueService.initializeQueues(request, auth)));
    }

    // FR-314: 순번 수동 조정 (사유 필수)
    @PatchMapping("/api/caddie/caddies/{caddieId}/queue")
    public ResponseEntity<ApiResponse<QueueRes>> adjustQueue(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long caddieId,
            @Valid @RequestBody AdjustQueueReq request
    ) {
        return ResponseEntity.ok(ApiResponse.success(queueService.adjustQueue(caddieId, request, auth)));
    }
}
