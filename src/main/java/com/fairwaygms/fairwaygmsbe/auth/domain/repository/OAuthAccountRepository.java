package com.fairwaygms.fairwaygmsbe.auth.domain.repository;

import com.fairwaygms.fairwaygmsbe.auth.domain.entity.OAuthAccount;
import com.fairwaygms.fairwaygmsbe.auth.domain.enums.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {

    Optional<OAuthAccount> findByProviderAndProviderIdAndIsDeletedFalse(OAuthProvider provider, String providerId);
    List<OAuthAccount> findByUserIdAndIsDeletedFalse(Long userId);
}
