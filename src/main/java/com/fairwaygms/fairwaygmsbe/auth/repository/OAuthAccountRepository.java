package com.fairwaygms.fairwaygmsbe.auth.repository;

import com.fairwaygms.fairwaygmsbe.auth.domain.OAuthAccount;
import com.fairwaygms.fairwaygmsbe.auth.domain.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {

    // 소셜 제공자 계정 기준 연동 조회
    Optional<OAuthAccount> findByProviderAndProviderIdAndIsDeletedFalse(
            OAuthProvider provider,
            String providerId
    );

    // 사용자별 소셜 연동 목록 조회
    List<OAuthAccount> findByUserIdAndIsDeletedFalse(Long userId);
}
