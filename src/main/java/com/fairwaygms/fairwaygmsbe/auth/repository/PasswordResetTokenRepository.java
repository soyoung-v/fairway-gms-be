package com.fairwaygms.fairwaygmsbe.auth.repository;

import com.fairwaygms.fairwaygmsbe.auth.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    // 비밀번호 재설정 링크 검증용 토큰 조회
    Optional<PasswordResetToken> findByTokenHashAndIsUsedFalseAndIsDeletedFalse(String tokenHash);
}
