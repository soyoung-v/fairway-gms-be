package com.fairwaygms.fairwaygmsbe.auth.application.service;

import com.fairwaygms.fairwaygmsbe.auth.application.model.req.OAuthCompleteReq;
import com.fairwaygms.fairwaygmsbe.auth.application.model.res.AuthUserRes;
import com.fairwaygms.fairwaygmsbe.auth.application.model.res.OAuthCompleteRes;
import com.fairwaygms.fairwaygmsbe.auth.domain.entity.OAuthAccount;
import com.fairwaygms.fairwaygmsbe.auth.domain.entity.RefreshToken;
import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.enums.OAuthProvider;
import com.fairwaygms.fairwaygmsbe.auth.domain.enums.UserStatus;
import com.fairwaygms.fairwaygmsbe.auth.domain.repository.OAuthAccountRepository;
import com.fairwaygms.fairwaygmsbe.auth.domain.repository.RefreshTokenRepository;
import com.fairwaygms.fairwaygmsbe.auth.domain.repository.UserRepository;
import com.fairwaygms.fairwaygmsbe.auth.exception.AuthErrorCode;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.security.JwtProperties;
import com.fairwaygms.fairwaygmsbe.common.security.JwtTokenProvider;
import com.fairwaygms.fairwaygmsbe.common.security.TokenHashProvider;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OAuthService {

    private final OAuthAccountRepository oAuthAccountRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final TokenHashProvider tokenHashProvider;

    // 소셜 인증 결과 유형 — 성공 핸들러가 리다이렉트 분기에 사용
    public enum LoginOutcomeType {LOGIN, PENDING, SIGNUP_REQUIRED}

    public record OAuthLoginOutcome(LoginOutcomeType type, AuthLoginResult loginResult) {
    }

    // 카카오 인증 완료 후 기존 연동 여부 판단 — 연동 있으면 로그인, 없으면 가입 필요
    @Transactional
    public OAuthLoginOutcome handleProviderLogin(OAuthProvider provider, String providerId) {
        Optional<OAuthAccount> accountOpt =
                oAuthAccountRepository.findByProviderAndProviderIdAndIsDeletedFalse(provider, providerId);
        if (accountOpt.isEmpty()) {
            return new OAuthLoginOutcome(LoginOutcomeType.SIGNUP_REQUIRED, null);
        }

        User user = userRepository.findByIdAndIsDeletedFalse(accountOpt.get().getUserId())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

        // 가입 승인 대기 중이면 토큰 없이 대기 안내로 리다이렉트
        if (user.getStatus() == UserStatus.PENDING) {
            return new OAuthLoginOutcome(LoginOutcomeType.PENDING, null);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(AuthErrorCode.ACCOUNT_WITHDRAWN);
        }

        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(), user.getRole(), user.getGolfCourseId());
        String refreshToken = jwtTokenProvider.createRefreshToken(
                user.getId(), user.getRole(), user.getGolfCourseId());
        saveRefreshToken(user.getId(), refreshToken);
        user.recordLogin();

        return new OAuthLoginOutcome(LoginOutcomeType.LOGIN,
                new AuthLoginResult(AuthUserRes.from(user), accessToken, refreshToken));
    }

    // API-115 (FR-115): 소셜 최초 가입 완료 — 임시 토큰 검증 후 PENDING 계정 + 연동 생성
    @Transactional
    public OAuthCompleteRes completeSignup(String signupToken, OAuthCompleteReq req) {
        JwtTokenProvider.OAuthSignupClaims claims = jwtTokenProvider.parseOAuthSignupToken(signupToken);
        if (claims == null) {
            throw new BusinessException(AuthErrorCode.OAUTH_STATE_INVALID);
        }

        OAuthProvider provider = OAuthProvider.valueOf(claims.provider());
        if (oAuthAccountRepository
                .findByProviderAndProviderIdAndIsDeletedFalse(provider, claims.providerId())
                .isPresent()) {
            throw new BusinessException(AuthErrorCode.OAUTH_STATE_INVALID, "이미 가입된 소셜 계정입니다.");
        }

        if (req.role() != UserRole.MANAGER && req.role() != UserRole.CADDY) {
            throw new BusinessException(AuthErrorCode.OAUTH_STATE_INVALID, "가입 가능한 역할이 아닙니다.");
        }

        // 이메일 우선순위: 가입 폼 직접 입력 > 카카오 제공 > provider 기반 대체 이메일 (email 컬럼 NOT NULL·UNIQUE)
        String email;
        if (StringUtils.hasText(req.email())) {
            email = req.email().trim().toLowerCase();
        } else if (StringUtils.hasText(claims.email())) {
            email = claims.email().trim().toLowerCase();
        } else {
            email = provider.name().toLowerCase() + "_" + claims.providerId() + "@social.fairway";
        }
        if (userRepository.existsByEmailAndIsDeletedFalse(email)) {
            throw new BusinessException(AuthErrorCode.EMAIL_DUPLICATED);
        }

        String name = StringUtils.hasText(req.name()) ? req.name() : claims.name();
        User user = userRepository.save(User.createSocialUser(
                email, name, req.phone(), req.role(), req.golfCourseId()));
        oAuthAccountRepository.save(OAuthAccount.create(
                user.getId(), provider, claims.providerId(), claims.email()));

        return new OAuthCompleteRes(user.getId(), user.getStatus().name());
    }

    private void saveRefreshToken(Long userId, String rawRefreshToken) {
        String tokenHash = tokenHashProvider.hash(rawRefreshToken);
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(jwtProperties.getRefreshTokenValiditySeconds());
        refreshTokenRepository.save(RefreshToken.create(userId, tokenHash, expiresAt));
    }
}
