package com.fairwaygms.fairwaygmsbe.notification.application.controller;

import com.fairwaygms.fairwaygmsbe.common.response.ApiResponse;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.notification.application.model.req.DeleteFcmTokenReq;
import com.fairwaygms.fairwaygmsbe.notification.application.model.req.RegisterFcmTokenReq;
import com.fairwaygms.fairwaygmsbe.notification.application.model.req.UpdateNotificationSettingReq;
import com.fairwaygms.fairwaygmsbe.notification.application.model.res.NotificationRes;
import com.fairwaygms.fairwaygmsbe.notification.application.model.res.NotificationSettingRes;
import com.fairwaygms.fairwaygmsbe.notification.application.service.FcmTokenService;
import com.fairwaygms.fairwaygmsbe.notification.application.service.NotificationService;
import com.fairwaygms.fairwaygmsbe.notification.application.service.NotificationSettingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "알림")
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationSettingService notificationSettingService;
    private final FcmTokenService fcmTokenService;

    // API-701: 알림 목록 조회
    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<Page<NotificationRes>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getNotifications(page, size, auth)));
    }

    // API-702: 미읽음 알림 수 조회
    @GetMapping("/notifications/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal AuthenticatedUser auth) {
        long count = notificationService.getUnreadCount(auth);
        return ResponseEntity.ok(ApiResponse.success(Map.of("unreadCount", count)));
    }

    // API-703: 알림 읽음 처리
    @PatchMapping("/notifications/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationRes>> markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.markAsRead(notificationId, auth)));
    }

    // API-704: 전체 읽음 처리
    @PatchMapping("/notifications/read-all")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsRead(
            @AuthenticationPrincipal AuthenticatedUser auth) {
        int updated = notificationService.markAllAsRead(auth);
        return ResponseEntity.ok(ApiResponse.success(Map.of("updatedCount", updated)));
    }

    // API-705: FCM 토큰 등록
    @PostMapping("/fcm/tokens")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> registerToken(
            @Valid @RequestBody RegisterFcmTokenReq req,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        boolean result = fcmTokenService.registerToken(req, auth);
        return ResponseEntity.ok(ApiResponse.success(Map.of("tokenRegistered", result)));
    }

    // API-706: FCM 토큰 해제
    @DeleteMapping("/fcm/tokens")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> deleteToken(
            @Valid @RequestBody DeleteFcmTokenReq req,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        boolean result = fcmTokenService.deleteToken(req, auth);
        return ResponseEntity.ok(ApiResponse.success(Map.of("tokenDeleted", result)));
    }

    // API-707: 알림 수신 설정 수정
    @PatchMapping("/settings")
    public ResponseEntity<ApiResponse<NotificationSettingRes>> updateSettings(
            @RequestBody UpdateNotificationSettingReq req,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationSettingService.updateSettings(req, auth)));
    }

    // API-708: 알림 수신 설정 조회
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<NotificationSettingRes>> getSettings(
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationSettingService.getSettings(auth)));
    }
}
