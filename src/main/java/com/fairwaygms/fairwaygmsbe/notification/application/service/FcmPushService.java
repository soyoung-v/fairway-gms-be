package com.fairwaygms.fairwaygmsbe.notification.application.service;

import java.util.List;

public interface FcmPushService {

    void sendPush(Long userId, String title, String content);

    void sendPushToAll(List<Long> userIds, String title, String content);
}
