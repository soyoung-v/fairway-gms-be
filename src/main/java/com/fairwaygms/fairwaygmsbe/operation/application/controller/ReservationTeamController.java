package com.fairwaygms.fairwaygmsbe.operation.application.controller;

import com.fairwaygms.fairwaygmsbe.common.response.ApiResponse;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.ChangeTeeTimeReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.CreateReservationTeamReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.SetDesignatedCaddieReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.UpdateReservationTeamReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.UpdateVipReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.res.ReservationTeamDetailRes;
import com.fairwaygms.fairwaygmsbe.operation.application.model.res.ReservationTeamRes;
import com.fairwaygms.fairwaygmsbe.operation.application.service.ReservationTeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/operation/reservation-teams")
public class ReservationTeamController {

    private final ReservationTeamService reservationTeamService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReservationTeamRes>> createTeam(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @Valid @RequestBody CreateReservationTeamReq request
    ) {
        return ResponseEntity.ok(ApiResponse.success(reservationTeamService.createTeam(request, auth)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReservationTeamRes>>> listTeams(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate playDate,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Integer periodNumber
    ) {
        return ResponseEntity.ok(ApiResponse.success(reservationTeamService.listTeams(playDate, courseId, periodNumber, auth)));
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<ApiResponse<ReservationTeamDetailRes>> getTeam(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long teamId
    ) {
        return ResponseEntity.ok(ApiResponse.success(reservationTeamService.getTeam(teamId, auth)));
    }

    @PatchMapping("/{teamId}")
    public ResponseEntity<ApiResponse<ReservationTeamRes>> updateTeam(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long teamId,
            @Valid @RequestBody UpdateReservationTeamReq request
    ) {
        return ResponseEntity.ok(ApiResponse.success(reservationTeamService.updateTeam(teamId, request, auth)));
    }

    @PatchMapping("/{teamId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelTeam(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long teamId
    ) {
        reservationTeamService.cancelTeam(teamId, auth);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PatchMapping("/{teamId}/no-show")
    public ResponseEntity<ApiResponse<Void>> noShow(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long teamId
    ) {
        reservationTeamService.noShow(teamId, auth);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PatchMapping("/{teamId}/rain-cancel")
    public ResponseEntity<ApiResponse<Void>> rainCancel(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long teamId
    ) {
        reservationTeamService.rainCancel(teamId, auth);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PatchMapping("/{teamId}/complete")
    public ResponseEntity<ApiResponse<Void>> complete(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long teamId
    ) {
        reservationTeamService.complete(teamId, auth);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PatchMapping("/{teamId}/designated-caddie")
    public ResponseEntity<ApiResponse<ReservationTeamRes>> setDesignatedCaddie(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long teamId,
            @Valid @RequestBody SetDesignatedCaddieReq request
    ) {
        return ResponseEntity.ok(ApiResponse.success(reservationTeamService.setDesignatedCaddie(teamId, request, auth)));
    }

    @PatchMapping("/{teamId}/vip")
    public ResponseEntity<ApiResponse<ReservationTeamRes>> updateVip(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long teamId,
            @Valid @RequestBody UpdateVipReq request
    ) {
        return ResponseEntity.ok(ApiResponse.success(reservationTeamService.updateVip(teamId, request, auth)));
    }

    @PatchMapping("/{teamId}/tee-time")
    public ResponseEntity<ApiResponse<ReservationTeamRes>> changeTeeTime(
            @AuthenticationPrincipal AuthenticatedUser auth,
            @PathVariable Long teamId,
            @Valid @RequestBody ChangeTeeTimeReq request
    ) {
        return ResponseEntity.ok(ApiResponse.success(reservationTeamService.changeTeeTime(teamId, request, auth)));
    }
}
