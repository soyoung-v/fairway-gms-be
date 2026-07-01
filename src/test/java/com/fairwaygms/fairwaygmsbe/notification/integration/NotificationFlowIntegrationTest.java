package com.fairwaygms.fairwaygmsbe.notification.integration;

import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.repository.UserRepository;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.GolfCourseRepository;
import com.fairwaygms.fairwaygmsbe.notification.application.model.req.DeleteFcmTokenReq;
import com.fairwaygms.fairwaygmsbe.notification.application.model.req.RegisterFcmTokenReq;
import com.fairwaygms.fairwaygmsbe.notification.application.model.req.UpdateNotificationSettingReq;
import com.fairwaygms.fairwaygmsbe.notification.application.model.res.NotificationRes;
import com.fairwaygms.fairwaygmsbe.notification.application.model.res.NotificationSettingRes;
import com.fairwaygms.fairwaygmsbe.notification.application.service.FcmTokenService;
import com.fairwaygms.fairwaygmsbe.notification.application.service.NotificationService;
import com.fairwaygms.fairwaygmsbe.notification.application.service.NotificationSettingService;
import com.fairwaygms.fairwaygmsbe.notification.domain.entity.Notification;
import com.fairwaygms.fairwaygmsbe.notification.domain.enums.DeviceType;
import com.fairwaygms.fairwaygmsbe.notification.domain.enums.NotificationType;
import com.fairwaygms.fairwaygmsbe.notification.domain.enums.ReferenceType;
import com.fairwaygms.fairwaygmsbe.notification.domain.repository.FcmTokenRepository;
import com.fairwaygms.fairwaygmsbe.notification.exception.NotificationErrorCode;
import com.fairwaygms.fairwaygmsbe.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;

class NotificationFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired NotificationService notificationService;
    @Autowired NotificationSettingService notificationSettingService;
    @Autowired FcmTokenService fcmTokenService;
    @Autowired GolfCourseRepository golfCourseRepository;
    @Autowired UserRepository userRepository;
    @Autowired FcmTokenRepository fcmTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private GolfCourse golfCourse;
    private AuthenticatedUser caddieAuth;
    private AuthenticatedUser otherAuth;

    @BeforeEach
    void setUp() {
        golfCourse = golfCourseRepository.save(GolfCourse.create("테스트CC", "서울시", "02-0000-0000"));

        User caddieUser = User.createEmailUser(
                "caddie@test.com", passwordEncoder.encode("Caddie1!"), "테스트캐디",
                null, UserRole.CADDY, golfCourse.getId());
        caddieUser.approve(999L);
        caddieUser = userRepository.save(caddieUser);
        caddieAuth = new AuthenticatedUser(caddieUser.getId(), UserRole.CADDY, golfCourse.getId());

        User otherUser = User.createEmailUser(
                "other@test.com", passwordEncoder.encode("Other1!"), "다른캐디",
                null, UserRole.CADDY, golfCourse.getId());
        otherUser.approve(999L);
        otherUser = userRepository.save(otherUser);
        otherAuth = new AuthenticatedUser(otherUser.getId(), UserRole.CADDY, golfCourse.getId());
    }

    // ─── 알림 목록/읽음 ─────────────────────────────────────────────────────────

    @Test
    void 알림_생성_후_목록_조회() {
        // given
        notificationService.createNotification(
                golfCourse.getId(), caddieAuth.getUserId(),
                NotificationType.ASSIGNMENT_CONFIRMED, "배정 확정", "7월 1일 배정이 확정되었습니다.",
                1L, ReferenceType.ASSIGNMENT);

        // when
        Page<NotificationRes> result = notificationService.getNotifications(0, 20, caddieAuth);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).type()).isEqualTo("ASSIGNMENT_CONFIRMED");
        assertThat(result.getContent().get(0).isRead()).isFalse();
    }

    @Test
    void 미읽음_알림_수_조회() {
        // given
        notificationService.createNotification(golfCourse.getId(), caddieAuth.getUserId(),
                NotificationType.ASSIGNMENT_CONFIRMED, "제목1", "내용1", null, null);
        notificationService.createNotification(golfCourse.getId(), caddieAuth.getUserId(),
                NotificationType.BOARD_POST_CREATED, "제목2", "내용2", null, null);

        // when
        long count = notificationService.getUnreadCount(caddieAuth);

        // then
        assertThat(count).isEqualTo(2L);
    }

    @Test
    void 단건_알림_읽음_처리() {
        // given
        Notification n = notificationService.createNotification(
                golfCourse.getId(), caddieAuth.getUserId(),
                NotificationType.SWAP_RESULT, "교환 결과", "승인되었습니다.", null, null);

        // when
        NotificationRes result = notificationService.markAsRead(n.getId(), caddieAuth);

        // then
        assertThat(result.isRead()).isTrue();
        assertThat(notificationService.getUnreadCount(caddieAuth)).isEqualTo(0L);
    }

    @Test
    void 전체_읽음_처리() {
        // given
        notificationService.createNotification(golfCourse.getId(), caddieAuth.getUserId(),
                NotificationType.ASSIGNMENT_CONFIRMED, "제목1", "내용1", null, null);
        notificationService.createNotification(golfCourse.getId(), caddieAuth.getUserId(),
                NotificationType.ASSIGNMENT_CHANGED, "제목2", "내용2", null, null);

        // when
        int updated = notificationService.markAllAsRead(caddieAuth);

        // then
        assertThat(updated).isEqualTo(2);
        assertThat(notificationService.getUnreadCount(caddieAuth)).isEqualTo(0L);
    }

    @Test
    void 다른_사용자_알림_읽음_처리_시_예외() {
        // given
        Notification n = notificationService.createNotification(
                golfCourse.getId(), caddieAuth.getUserId(),
                NotificationType.ASSIGNMENT_CONFIRMED, "제목", "내용", null, null);

        // when / then — otherAuth가 caddieAuth의 알림을 읽으려 함
        assertThatThrownBy(() -> notificationService.markAsRead(n.getId(), otherAuth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(NotificationErrorCode.NOTIFICATION_ACCESS_DENIED));
    }

    // ─── FCM 토큰 ───────────────────────────────────────────────────────────────

    @Test
    void FCM_토큰_등록_및_해제() {
        String token = "test-fcm-token-xyz";

        // 등록
        boolean registered = fcmTokenService.registerToken(
                new RegisterFcmTokenReq(token, DeviceType.MOBILE), caddieAuth);
        assertThat(registered).isTrue();
        assertThat(fcmTokenRepository.findByTokenAndIsDeletedFalse(token)).isPresent();

        // 해제
        boolean deleted = fcmTokenService.deleteToken(new DeleteFcmTokenReq(token), caddieAuth);
        assertThat(deleted).isTrue();
        assertThat(fcmTokenRepository.findByTokenAndIsDeletedFalse(token)).isEmpty();
    }

    // ─── 알림 설정 ──────────────────────────────────────────────────────────────

    @Test
    void 알림_설정_조회_시_없으면_기본값_생성_후_반환() {
        // when
        NotificationSettingRes result = notificationSettingService.getSettings(caddieAuth);

        // then — 기본값 모두 true
        assertThat(result.isAssignmentEnabled()).isTrue();
        assertThat(result.isBoardEnabled()).isTrue();
        assertThat(result.isSwapEnabled()).isTrue();
    }

    @Test
    void 알림_설정_수정() {
        // given — 초기 조회로 설정 생성
        notificationSettingService.getSettings(caddieAuth);

        // when
        NotificationSettingRes result = notificationSettingService.updateSettings(
                new UpdateNotificationSettingReq(false, true, false), caddieAuth);

        // then
        assertThat(result.isAssignmentEnabled()).isFalse();
        assertThat(result.isBoardEnabled()).isTrue();
        assertThat(result.isSwapEnabled()).isFalse();
    }
}
