package com.fairwaygms.fairwaygmsbe.caddie.application.controller;

import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.MyCaddieRes;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.MyQueueRes;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.MySchedulePeriodRes;
import com.fairwaygms.fairwaygmsbe.caddie.application.service.CaddieMobileService;
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
@RequestMapping("/api/caddie/me")
public class CaddieMobileController {

    private final CaddieMobileService caddieMobileService;

    // FR-325: 내 기본정보 + 근무패턴 조회 (Caddy)
    @GetMapping
    public ResponseEntity<ApiResponse<MyCaddieRes>> getMyInfo(
            @AuthenticationPrincipal AuthenticatedUser auth
    ) {
        return ResponseEntity.ok(ApiResponse.success(caddieMobileService.getMyInfo(auth)));
    }

    // FR-326: 내 대기 순번 조회 (Caddy) — queueDate 생략 시 오늘 기준
    @GetMapping("/queue")
    public ResponseEntity<ApiResponse<MyQueueRes>> getMyQueue(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate queueDate
    ) {
        return ResponseEntity.ok(ApiResponse.success(caddieMobileService.getMyQueue(auth, queueDate)));
    }

    // API-320 (FR-327): 내 운영 시간표 조회 (Caddy) — targetDate 생략 시 오늘 기준
    @GetMapping("/schedule")
    public ResponseEntity<ApiResponse<List<MySchedulePeriodRes>>> getMySchedule(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
    ) {
        return ResponseEntity.ok(ApiResponse.success(caddieMobileService.getMySchedule(auth, targetDate)));
    }
}
