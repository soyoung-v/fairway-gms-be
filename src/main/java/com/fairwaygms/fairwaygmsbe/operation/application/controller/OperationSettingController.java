package com.fairwaygms.fairwaygmsbe.operation.application.controller;

import com.fairwaygms.fairwaygmsbe.common.response.ApiResponse;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.CreateOperationSettingReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.UpdateOperationSettingReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.res.OperationSettingRes;
import com.fairwaygms.fairwaygmsbe.operation.application.service.OperationSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/operation/settings")
public class OperationSettingController {

    private final OperationSettingService operationSettingService;

    @PostMapping
    public ResponseEntity<ApiResponse<OperationSettingRes>> createSetting(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @Valid @RequestBody CreateOperationSettingReq request
    ) {
        return ResponseEntity.ok(ApiResponse.success(operationSettingService.createSetting(request, auth)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<OperationSettingRes>> getSetting(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @RequestParam String yearMonth
    ) {
        return ResponseEntity.ok(ApiResponse.success(operationSettingService.getSetting(yearMonth, auth)));
    }

    @PatchMapping("/{settingId}")
    public ResponseEntity<ApiResponse<OperationSettingRes>> updateSetting(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long settingId,
            @Valid @RequestBody UpdateOperationSettingReq request
    ) {
        return ResponseEntity.ok(ApiResponse.success(operationSettingService.updateSetting(settingId, request, auth)));
    }
}
