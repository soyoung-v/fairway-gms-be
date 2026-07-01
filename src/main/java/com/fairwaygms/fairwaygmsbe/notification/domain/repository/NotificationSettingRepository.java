package com.fairwaygms.fairwaygmsbe.notification.domain.repository;

import com.fairwaygms.fairwaygmsbe.notification.domain.entity.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    Optional<NotificationSetting> findByUserIdAndIsDeletedFalse(Long userId);
}
