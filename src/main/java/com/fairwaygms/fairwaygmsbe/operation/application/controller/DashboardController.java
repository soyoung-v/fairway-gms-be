package com.fairwaygms.fairwaygmsbe.operation.application.controller;

import com.fairwaygms.fairwaygmsbe.common.response.ApiResponse;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.operation.application.model.res.DashboardRes;
import com.fairwaygms.fairwaygmsbe.operation.application.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/operation/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardRes>> getDashboard(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
    ) {
        LocalDate date = targetDate != null ? targetDate : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDashboard(date, auth)));
    }
}
