package com.fairwaygms.fairwaygmsbe.operation.application.controller;

import com.fairwaygms.fairwaygmsbe.common.response.ApiResponse;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.CreateSpecialDayReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.UpdateRainPolicyReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.res.RainPolicyRes;
import com.fairwaygms.fairwaygmsbe.operation.application.model.res.SpecialDayRes;
import com.fairwaygms.fairwaygmsbe.operation.application.service.OperationPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/operation")
public class OperationPolicyController {

    private final OperationPolicyService operationPolicyService;

    @GetMapping("/special-days")
    public ResponseEntity<ApiResponse<List<SpecialDayRes>>> listSpecialDays(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @RequestParam String yearMonth
    ) {
        return ResponseEntity.ok(ApiResponse.success(operationPolicyService.listSpecialDays(yearMonth, auth)));
    }

    @PostMapping("/special-days")
    public ResponseEntity<ApiResponse<SpecialDayRes>> createSpecialDay(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @Valid @RequestBody CreateSpecialDayReq request
    ) {
        return ResponseEntity.ok(ApiResponse.success(operationPolicyService.createSpecialDay(request, auth)));
    }

    @DeleteMapping("/special-days/{specialDayId}")
    public ResponseEntity<ApiResponse<Void>> deleteSpecialDay(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long specialDayId
    ) {
        operationPolicyService.deleteSpecialDay(specialDayId, auth);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PutMapping("/policies/rain-cancellation")
    public ResponseEntity<ApiResponse<RainPolicyRes>> upsertRainPolicy(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @Valid @RequestBody UpdateRainPolicyReq request
    ) {
        return ResponseEntity.ok(ApiResponse.success(operationPolicyService.upsertRainPolicy(request, auth)));
    }

    @GetMapping("/policies/rain-cancellation")
    public ResponseEntity<ApiResponse<RainPolicyRes>> getRainPolicy(
            @AuthenticationPrincipal AuthenticatedUser auth
    ) {
        return ResponseEntity.ok(ApiResponse.success(operationPolicyService.getRainPolicy(auth)));
    }
}
