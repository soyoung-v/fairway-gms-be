package com.fairwaygms.fairwaygmsbe.notification.application.service;

import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import com.fairwaygms.fairwaygmsbe.notification.application.model.req.DeleteFcmTokenReq;
import com.fairwaygms.fairwaygmsbe.notification.application.model.req.RegisterFcmTokenReq;
import com.fairwaygms.fairwaygmsbe.notification.domain.entity.FcmToken;
import com.fairwaygms.fairwaygmsbe.notification.domain.enums.DeviceType;
import com.fairwaygms.fairwaygmsbe.notification.domain.repository.FcmTokenRepository;
import com.fairwaygms.fairwaygmsbe.notification.exception.NotificationErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FcmTokenServiceTest {

    @Mock private FcmTokenRepository fcmTokenRepository;

    private FcmTokenService fcmTokenService;

    private static final Long USER_ID = 1L;
    private static final Long GOLF_COURSE_ID = 100L;
    private static final String TOKEN = "fcm-token-abc123";

    private AuthenticatedUser auth;

    @BeforeEach
    void setUp() {
        fcmTokenService = new FcmTokenService(fcmTokenRepository);
        auth = new AuthenticatedUser(USER_ID, UserRole.CADDY, GOLF_COURSE_ID);
    }

    @Test
    void registerToken_새_토큰을_DB에_저장한다() {
        // given
        when(fcmTokenRepository.findByTokenAndIsDeletedFalse(TOKEN)).thenReturn(Optional.empty());
        when(fcmTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        boolean result = fcmTokenService.registerToken(
                new RegisterFcmTokenReq(TOKEN, DeviceType.MOBILE), auth);

        // then
        assertThat(result).isTrue();
        verify(fcmTokenRepository).save(any(FcmToken.class));
    }

    @Test
    void registerToken_이미_존재하는_토큰은_lastUsedAt을_갱신한다() {
        // given
        FcmToken existing = spy(FcmToken.create(USER_ID, GOLF_COURSE_ID, TOKEN, DeviceType.MOBILE));
        when(fcmTokenRepository.findByTokenAndIsDeletedFalse(TOKEN)).thenReturn(Optional.of(existing));

        // when
        boolean result = fcmTokenService.registerToken(
                new RegisterFcmTokenReq(TOKEN, DeviceType.MOBILE), auth);

        // then
        assertThat(result).isTrue();
        verify(existing).updateLastUsed();
        verify(fcmTokenRepository, never()).save(any());
    }

    @Test
    void deleteToken_토큰을_소프트삭제한다() {
        // given
        FcmToken existing = spy(FcmToken.create(USER_ID, GOLF_COURSE_ID, TOKEN, DeviceType.MOBILE));
        when(fcmTokenRepository.findByTokenAndIsDeletedFalse(TOKEN)).thenReturn(Optional.of(existing));

        // when
        boolean result = fcmTokenService.deleteToken(new DeleteFcmTokenReq(TOKEN), auth);

        // then
        assertThat(result).isTrue();
        verify(existing).delete();
    }

    @Test
    void deleteToken_존재하지_않는_토큰_삭제_시_예외() {
        // given
        when(fcmTokenRepository.findByTokenAndIsDeletedFalse(TOKEN)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> fcmTokenService.deleteToken(new DeleteFcmTokenReq(TOKEN), auth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(NotificationErrorCode.FCM_TOKEN_NOT_FOUND));
    }
}
