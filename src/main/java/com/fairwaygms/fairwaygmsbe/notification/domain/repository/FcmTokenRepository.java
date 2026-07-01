package com.fairwaygms.fairwaygmsbe.notification.domain.repository;

import com.fairwaygms.fairwaygmsbe.notification.domain.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    Optional<FcmToken> findByTokenAndIsDeletedFalse(String token);

    List<FcmToken> findByUserIdAndIsActiveTrue(Long userId);
}
