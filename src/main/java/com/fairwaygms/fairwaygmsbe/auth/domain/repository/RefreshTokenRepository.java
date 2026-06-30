package com.fairwaygms.fairwaygmsbe.auth.domain.repository;

import com.fairwaygms.fairwaygmsbe.auth.domain.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHashAndIsDeletedFalse(String tokenHash);
    List<RefreshToken> findByUserIdAndIsRevokedFalseAndIsDeletedFalse(Long userId);
    List<RefreshToken> findByUserIdAndIsDeletedFalse(Long userId);
}
