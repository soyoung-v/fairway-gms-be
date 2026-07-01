package com.fairwaygms.fairwaygmsbe.notification.infrastructure;

import com.fairwaygms.fairwaygmsbe.notification.application.service.FcmPushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

// local/test 환경에서 실제 FCM 대신 로그만 출력하는 mock 구현체
@Slf4j
@Service
@Profile({"local", "test"})
public class MockFcmPushService implements FcmPushService {

    @Override
    public void sendPush(Long userId, String title, String content) {
        log.info("[FCM MOCK] userId={} title='{}' content='{}'", userId, title, content);
    }

    @Override
    public void sendPushToAll(List<Long> userIds, String title, String content) {
        log.info("[FCM MOCK] userIds={} title='{}' content='{}'", userIds, title, content);
    }
}
