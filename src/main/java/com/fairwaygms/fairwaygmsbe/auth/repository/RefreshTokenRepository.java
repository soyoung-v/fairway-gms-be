package com.fairwaygms.fairwaygmsbe.auth.repository;

import com.fairwaygms.fairwaygmsbe.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // 토큰 해시 기준 유효 후보 조회
    Optional<RefreshToken> findByTokenHashAndIsDeletedFalse(String tokenHash);

    // 사용자별 미폐기 리프레시 토큰 조회
    List<RefreshToken> findByUserIdAndIsRevokedFalseAndIsDeletedFalse(Long userId);
}
