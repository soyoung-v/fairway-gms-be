package com.fairwaygms.fairwaygmsbe.settlement.application.controller;

import com.fairwaygms.fairwaygmsbe.common.response.ApiResponse;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.settlement.application.model.req.*;
import com.fairwaygms.fairwaygmsbe.settlement.application.model.res.*;
import com.fairwaygms.fairwaygmsbe.settlement.application.service.FeePolicyService;
import com.fairwaygms.fairwaygmsbe.settlement.application.service.MonthlySettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settlement")
@RequiredArgsConstructor
public class SettlementController {

    private final FeePolicyService feePolicyService;
    private final MonthlySettlementService monthlySettlementService;

    // API-601: 캐디피 정책 등록/수정
    @PutMapping("/fee-policies")
    public ResponseEntity<ApiResponse<FeePolicyRes>> upsertFeePolicy(
            @Valid @RequestBody UpsertFeePolicyReq req,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return ResponseEntity.ok(ApiResponse.success(feePolicyService.upsertFeePolicy(req, auth)));
    }

    // API-602: 캐디피 정책 조회
    @GetMapping("/fee-policies")
    public ResponseEntity<ApiResponse<FeePolicyRes>> getFeePolicy(
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return ResponseEntity.ok(ApiResponse.success(feePolicyService.getFeePolicy(auth)));
    }

    // API-603: 우천취소 캐디피 계산
    @PostMapping("/fee-policies/rain-cancellation/calculate")
    public ResponseEntity<ApiResponse<FeeCalculationRes>> calculateRainCancellationFee(
            @Valid @RequestBody RainCancellationCalculateReq req,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return ResponseEntity.ok(ApiResponse.success(
                feePolicyService.calculateRainCancellationFee(req, auth)));
    }

    // API-604: 노쇼 캐디피 계산
    @PostMapping("/fee-policies/no-show/calculate")
    public ResponseEntity<ApiResponse<FeeCalculationRes>> calculateNoShowFee(
            @Valid @RequestBody NoShowCalculateReq req,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return ResponseEntity.ok(ApiResponse.success(
                feePolicyService.calculateNoShowFee(req, auth)));
    }

    // API-605: 캐디피 수동 조정
    @PatchMapping("/caddies/{caddieId}/fee")
    public ResponseEntity<ApiResponse<IncomeSummaryRes>> adjustCaddieFee(
            @PathVariable Long caddieId,
            @Valid @RequestBody AdjustCaddieFeeReq req,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return ResponseEntity.ok(ApiResponse.success(
                monthlySettlementService.adjustCaddieFee(caddieId, req, auth)));
    }

    // API-606: 캐디별 근무횟수 집계
    @GetMapping("/monthly/rounds")
    public ResponseEntity<ApiResponse<List<RoundSummaryRes>>> getRoundSummary(
            @RequestParam String yearMonth,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return ResponseEntity.ok(ApiResponse.success(
                monthlySettlementService.getRoundSummary(yearMonth, auth)));
    }

    // API-607: 캐디별 수입 집계
    @GetMapping("/monthly/income")
    public ResponseEntity<ApiResponse<List<IncomeSummaryRes>>> getIncomeSummary(
            @RequestParam String yearMonth,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return ResponseEntity.ok(ApiResponse.success(
                monthlySettlementService.getIncomeSummary(yearMonth, auth)));
    }

    // API-608: 월 마감 확정
    @PostMapping("/monthly/{yearMonth}/confirm")
    public ResponseEntity<ApiResponse<MonthlySettlementRes>> confirmMonth(
            @PathVariable String yearMonth,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return ResponseEntity.ok(ApiResponse.success(
                monthlySettlementService.confirmMonth(yearMonth, auth)));
    }

    // API-609: 월 마감 확정 취소 (Admin 전용)
    @DeleteMapping("/monthly/{yearMonth}/confirm")
    public ResponseEntity<ApiResponse<Void>> cancelConfirmMonth(
            @PathVariable String yearMonth,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        monthlySettlementService.cancelConfirmMonth(yearMonth, auth);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // API-610: 보험 자료 다운로드 (stub — Excel 구현은 별도 단계)
    @GetMapping("/insurance/export")
    public ResponseEntity<byte[]> exportInsurance(@RequestParam String yearMonth,
                                                  @AuthenticationPrincipal AuthenticatedUser auth) {
        throw new UnsupportedOperationException("보험 자료 Excel 다운로드 미구현");
    }

    // API-611: 정산 자료 Excel 다운로드 (stub)
    @GetMapping("/excel")
    public ResponseEntity<byte[]> exportSettlement(@RequestParam String yearMonth,
                                                   @AuthenticationPrincipal AuthenticatedUser auth) {
        throw new UnsupportedOperationException("정산 자료 Excel 다운로드 미구현");
    }

    // API-612: 정산 변경 이력 조회
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<SettlementHistoryRes>>> getHistory(
            @RequestParam String yearMonth,
            @RequestParam(required = false) Long caddieId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return ResponseEntity.ok(ApiResponse.success(
                monthlySettlementService.getHistory(yearMonth, caddieId, page, size, auth)));
    }
}
