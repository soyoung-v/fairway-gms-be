package com.fairwaygms.fairwaygmsbe.notification.infrastructure;

import com.fairwaygms.fairwaygmsbe.notification.application.service.FcmPushService;
import com.fairwaygms.fairwaygmsbe.notification.domain.entity.FcmToken;
import com.fairwaygms.fairwaygmsbe.notification.domain.repository.FcmTokenRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@Profile("prod")
@RequiredArgsConstructor
public class RealFcmPushService implements FcmPushService {

    private final FcmTokenRepository fcmTokenRepository;

    @Override
    public void sendPush(Long userId, String title, String content) {
        List<FcmToken> tokens = fcmTokenRepository.findByUserIdAndIsActiveTrue(userId);
        if (tokens.isEmpty()) {
            log.debug("FCM 토큰 없음 — userId={}", userId);
            return;
        }
        tokens.forEach(token -> send(token.getToken(), title, content));
    }

    @Override
    public void sendPushToAll(List<Long> userIds, String title, String content) {
        userIds.forEach(userId -> sendPush(userId, title, content));
    }

    private void send(String token, String title, String content) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("FirebaseApp 미초기화 — 발송 건너뜀 (token={})", token);
            return;
        }
        try {
            Message message = Message.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(content)
                            .build())
                    .setToken(token)
                    .build();
            String response = FirebaseMessaging.getInstance().send(message);
            log.debug("FCM 발송 성공: {}", response);
        } catch (Exception e) {
            log.error("FCM 발송 실패 (token={}): {}", token, e.getMessage());
        }
    }
}
