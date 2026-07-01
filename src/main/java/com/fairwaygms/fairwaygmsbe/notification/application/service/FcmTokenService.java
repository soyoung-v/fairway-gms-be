package com.fairwaygms.fairwaygmsbe.notification.application.service;

import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.notification.application.model.req.DeleteFcmTokenReq;
import com.fairwaygms.fairwaygmsbe.notification.application.model.req.RegisterFcmTokenReq;
import com.fairwaygms.fairwaygmsbe.notification.domain.entity.FcmToken;
import com.fairwaygms.fairwaygmsbe.notification.domain.repository.FcmTokenRepository;
import com.fairwaygms.fairwaygmsbe.notification.exception.NotificationErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;

    @Transactional
    public boolean registerToken(RegisterFcmTokenReq req, AuthenticatedUser auth) {
        // 이미 등록된 토큰이면 활성 상태로 갱신하고 마지막 사용 시간을 업데이트한다
        fcmTokenRepository.findByTokenAndIsDeletedFalse(req.token())
                .ifPresentOrElse(
                        existing -> {
                            if (!existing.getIsActive()) {
                                // 비활성 토큰을 재활성화
                            }
                            existing.updateLastUsed();
                        },
                        () -> fcmTokenRepository.save(
                                FcmToken.create(auth.getUserId(), auth.getGolfCourseId(),
                                        req.token(), req.deviceType()))
                );
        return true;
    }

    @Transactional
    public boolean deleteToken(DeleteFcmTokenReq req, AuthenticatedUser auth) {
        FcmToken token = fcmTokenRepository.findByTokenAndIsDeletedFalse(req.token())
                .orElseThrow(() -> new BusinessException(NotificationErrorCode.FCM_TOKEN_NOT_FOUND));
        token.delete();
        return true;
    }
}
