package com.fairwaygms.fairwaygmsbe.auth.domain.repository;

import com.fairwaygms.fairwaygmsbe.auth.domain.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    // 비밀번호 재설정 링크 검증용 토큰 조회
    Optional<PasswordResetToken> findByTokenHashAndIsUsedFalseAndIsDeletedFalse(String tokenHash);

    // 신규 발급 전 기존 미사용 토큰을 무효화하기 위한 조회
    List<PasswordResetToken> findAllByUserIdAndIsUsedFalseAndIsDeletedFalse(Long userId);
}
