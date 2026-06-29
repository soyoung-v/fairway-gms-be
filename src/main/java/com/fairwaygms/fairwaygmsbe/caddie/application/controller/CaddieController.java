package com.fairwaygms.fairwaygmsbe.caddie.application.controller;

import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.ChangeCaddieStatusReq;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.SetDesignatedCartReq;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.UpdateCaddieReq;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.UpdateWorkPatternReq;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.AvailableCaddieRes;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.CaddieRes;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.CaddieWithdrawRes;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.DesignatedCartRes;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.WorkPatternRes;
import com.fairwaygms.fairwaygmsbe.caddie.application.service.CaddieService;
import com.fairwaygms.fairwaygmsbe.caddie.application.service.DesignatedCartService;
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
@RequestMapping("/api/caddie/caddies")
public class CaddieController {

    private final CaddieService caddieService;
    private final DesignatedCartService designatedCartService;

    // FR-302: 골프장별 캐디 목록 (Admin: golfCourseId 필수, Manager: 소속 골프장 고정)
    @GetMapping
    public ResponseEntity<ApiResponse<List<CaddieRes>>> getList(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @RequestParam(required = false) Long golfCourseId
    ) {
        return ResponseEntity.ok(ApiResponse.success(caddieService.getList(golfCourseId, auth)));
    }

    // FR-303: 캐디 상세 조회
    @GetMapping("/{caddieId}")
    public ResponseEntity<ApiResponse<CaddieRes>> getDetail(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long caddieId
    ) {
        return ResponseEntity.ok(ApiResponse.success(caddieService.getDetail(caddieId, auth)));
    }

    // FR-304: 캐디 정보 수정 (Manager)
    @PatchMapping("/{caddieId}")
    public ResponseEntity<ApiResponse<CaddieRes>> updateInfo(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long caddieId,
            @Valid @RequestBody UpdateCaddieReq request
    ) {
        return ResponseEntity.ok(ApiResponse.success(caddieService.updateInfo(caddieId, request, auth)));
    }

    // FR-305: 캐디 상태 변경 (Manager) — RESIGNED 제외
    @PatchMapping("/{caddieId}/status")
    public ResponseEntity<ApiResponse<CaddieRes>> changeStatus(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long caddieId,
            @Valid @RequestBody ChangeCaddieStatusReq request
    ) {
        return ResponseEntity.ok(ApiResponse.success(caddieService.changeStatus(caddieId, request, auth)));
    }

    // FR-118: 캐디 퇴사 처리 (Manager) — caddie + user 동시 비활성화
    @PatchMapping("/{caddieId}/withdraw")
    public ResponseEntity<ApiResponse<CaddieWithdrawRes>> withdrawCaddie(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long caddieId
    ) {
        return ResponseEntity.ok(ApiResponse.success(caddieService.withdrawCaddie(caddieId, auth)));
    }

    // FR-324: 가용 캐디 조회 (Manager)
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<AvailableCaddieRes>>> getAvailableCaddies(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @RequestParam(required = false) Long golfCourseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                caddieService.getAvailableCaddies(golfCourseId, date, auth)));
    }

    // FR-310/311: 근무 패턴 수정 (Manager)
    @PatchMapping("/{caddieId}/work-pattern")
    public ResponseEntity<ApiResponse<WorkPatternRes>> updateWorkPattern(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long caddieId,
            @RequestBody UpdateWorkPatternReq request
    ) {
        return ResponseEntity.ok(ApiResponse.success(caddieService.updateWorkPattern(caddieId, request, auth)));
    }

    // FR-309: 캐디별 지정카트 목록 조회 (Manager)
    @GetMapping("/designated-carts")
    public ResponseEntity<ApiResponse<List<DesignatedCartRes>>> getDesignatedCarts(
            @AuthenticationPrincipal AuthenticatedUser auth
    ) {
        return ResponseEntity.ok(ApiResponse.success(designatedCartService.getDesignatedCarts(auth)));
    }

    // FR-307: 지정카트 설정 (Manager)
    @PostMapping("/{caddieId}/designated-cart")
    public ResponseEntity<ApiResponse<DesignatedCartRes>> setDesignatedCart(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long caddieId,
            @Valid @RequestBody SetDesignatedCartReq request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(designatedCartService.setDesignatedCart(caddieId, request, auth)));
    }

    // FR-308: 지정카트 해제 (Manager)
    @DeleteMapping("/{caddieId}/designated-cart")
    public ResponseEntity<ApiResponse<Void>> removeDesignatedCart(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long caddieId
    ) {
        designatedCartService.removeDesignatedCart(caddieId, auth);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
