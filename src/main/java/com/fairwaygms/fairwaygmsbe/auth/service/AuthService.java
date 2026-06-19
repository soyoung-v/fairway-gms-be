package com.fairwaygms.fairwaygmsbe.auth.service;

import com.fairwaygms.fairwaygmsbe.auth.domain.PasswordResetToken;
import com.fairwaygms.fairwaygmsbe.auth.domain.RefreshToken;
import com.fairwaygms.fairwaygmsbe.auth.domain.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.UserStatus;
import com.fairwaygms.fairwaygmsbe.auth.dto.AuthUserResponse;
import com.fairwaygms.fairwaygmsbe.auth.dto.ChangePasswordRequest;
import com.fairwaygms.fairwaygmsbe.auth.dto.ForgotPasswordRequest;
import com.fairwaygms.fairwaygmsbe.auth.dto.LoginRequest;
import com.fairwaygms.fairwaygmsbe.auth.dto.MeResponse;
import com.fairwaygms.fairwaygmsbe.auth.dto.ResetPasswordRequest;
import com.fairwaygms.fairwaygmsbe.auth.dto.SignupRequest;
import com.fairwaygms.fairwaygmsbe.auth.dto.SignupResponse;
import com.fairwaygms.fairwaygmsbe.auth.repository.PasswordResetTokenRepository;
import com.fairwaygms.fairwaygmsbe.auth.repository.RefreshTokenRepository;
import com.fairwaygms.fairwaygmsbe.auth.repository.UserRepository;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.JwtProperties;
import com.fairwaygms.fairwaygmsbe.common.security.JwtTokenProvider;
import com.fairwaygms.fairwaygmsbe.common.security.TokenHashProvider;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long PASSWORD_RESET_VALIDITY_MINUTES = 30L;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final TokenHashProvider tokenHashProvider;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final EmailService emailService;

    // 회원가입은 승인 대기 계정만 생성하고 토큰은 발급하지 않는다.
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.email());
        validateSignupContext(request.role(), request.golfCourseId());
        passwordPolicyValidator.validate(request.password());

        if (userRepository.existsByEmailAndIsDeletedFalse(email)) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
        }

        User user = User.createEmailUser(
                email,
                passwordEncoder.encode(request.password()),
                request.name(),
                request.phone(),
                request.role(),
                request.golfCourseId()
        );
        User savedUser = userRepository.save(user);
        return SignupResponse.from(savedUser);
    }

    // ACTIVE 계정만 로그인시키고 Refresh Token은 해시로만 저장한다.
    @Transactional
    public AuthLoginResult login(LoginRequest request) {
        User user = userRepository.findByEmailAndIsDeletedFalse(normalizeEmail(request.email()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        validatePassword(user, request.password());
        validateLoginStatus(user);

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole(), user.getGolfCourseId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getRole(), user.getGolfCourseId());

        saveRefreshToken(user.getId(), refreshToken);
        user.recordLogin();

        return new AuthLoginResult(AuthUserResponse.from(user), accessToken, refreshToken);
    }

    // 로그아웃은 토큰이 없거나 이미 폐기되어도 클라이언트 관점에서는 성공 처리한다.
    @Transactional
    public void logout(String rawRefreshToken) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            return;
        }

        String tokenHash = tokenHashProvider.hash(rawRefreshToken);
        refreshTokenRepository.findByTokenHashAndIsDeletedFalse(tokenHash)
                .filter(refreshToken -> !Boolean.TRUE.equals(refreshToken.getIsRevoked()))
                .ifPresent(RefreshToken::revoke);
    }

    // Refresh Token Rotation: 기존 토큰 폐기 후 새 at/rt 쿠키용 토큰을 발급한다.
    @Transactional
    public AuthLoginResult refresh(String rawRefreshToken) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        if (!jwtTokenProvider.validateToken(rawRefreshToken)) {
            throw new BusinessException(jwtTokenProvider.isExpired(rawRefreshToken)
                    ? ErrorCode.REFRESH_TOKEN_EXPIRED
                    : ErrorCode.REFRESH_TOKEN_INVALID);
        }
        if (!jwtTokenProvider.isRefreshToken(rawRefreshToken)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        String tokenHash = tokenHashProvider.hash(rawRefreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHashAndIsDeletedFalse(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));
        validateRefreshTokenStatus(storedToken);

        User user = userRepository.findByIdAndIsDeletedFalse(jwtTokenProvider.getUserId(rawRefreshToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        validateLoginStatus(user);

        storedToken.revoke();
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole(), user.getGolfCourseId());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getRole(), user.getGolfCourseId());
        saveRefreshToken(user.getId(), newRefreshToken);

        return new AuthLoginResult(AuthUserResponse.from(user), newAccessToken, newRefreshToken);
    }

    // 이메일 사용 가능 여부 반환. true이면 가입 가능한 이메일이다.
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmailAndIsDeletedFalse(normalizeEmail(email));
    }

    // 현재 비밀번호 검증 후 새 비밀번호로 교체한다.
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        validatePassword(user, request.currentPassword());
        passwordPolicyValidator.validate(request.newPassword());
        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    // 이메일 미존재 여부는 노출하지 않는다 — 항상 성공 응답을 반환한다.
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());
        userRepository.findByEmailAndIsDeletedFalse(email).ifPresent(user -> {
            // 기존 미사용 토큰을 모두 무효화하고 새 토큰을 발급한다.
            List<PasswordResetToken> oldTokens =
                    passwordResetTokenRepository.findAllByUserIdAndIsUsedFalseAndIsDeletedFalse(user.getId());
            oldTokens.forEach(PasswordResetToken::softDelete);

            String rawToken = generateSecureToken();
            String tokenHash = tokenHashProvider.hash(rawToken);
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(PASSWORD_RESET_VALIDITY_MINUTES);
            passwordResetTokenRepository.save(PasswordResetToken.create(user.getId(), tokenHash, expiresAt));

            emailService.sendPasswordResetEmail(email, rawToken);
        });
    }

    // 토큰이 유효하면 비밀번호를 변경하고 토큰을 사용 완료 처리한다.
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String tokenHash = tokenHashProvider.hash(request.token());
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenHashAndIsUsedFalseAndIsDeletedFalse(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

        if (resetToken.isExpired()) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        User user = userRepository.findByIdAndIsDeletedFalse(resetToken.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        passwordPolicyValidator.validate(request.newPassword());
        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
        resetToken.markUsed();
    }

    // SecurityContext의 사용자 ID로 현재 계정을 조회한다.
    @Transactional(readOnly = true)
    public MeResponse getMe(Long userId) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return MeResponse.from(user);
    }

    // Manager/Caddy는 가입 시 소속 골프장이 필요하고 Admin은 null을 허용한다.
    private void validateSignupContext(UserRole role, Long golfCourseId) {
        if (role != UserRole.ADMIN && golfCourseId == null) {
            throw new BusinessException(ErrorCode.GOLF_COURSE_REQUIRED);
        }
    }

    // 비밀번호 해시가 없는 소셜 전용 계정은 이메일 비밀번호 로그인을 막는다.
    private void validatePassword(User user, String rawPassword) {
        if (!StringUtils.hasText(user.getPasswordHash())
                || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    // 승인 완료 계정만 로그인 허용
    private void validateLoginStatus(User user) {
        UserStatus status = user.getStatus();
        if (status == UserStatus.ACTIVE) {
            return;
        }
        if (status == UserStatus.LOCKED) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
        if (status == UserStatus.PENDING) {
            throw new BusinessException(ErrorCode.ACCOUNT_PENDING);
        }
        if (status == UserStatus.WITHDRAWN || status == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.ACCOUNT_WITHDRAWN);
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "활성 상태가 아닌 계정입니다.");
    }

    // DB에 저장된 Refresh Token 상태 검증
    private void validateRefreshTokenStatus(RefreshToken refreshToken) {
        if (Boolean.TRUE.equals(refreshToken.getIsRevoked())) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_REVOKED);
        }
        if (Boolean.TRUE.equals(refreshToken.getIsDeleted())) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }
    }

    // DB에는 Refresh Token 원문 대신 SHA-256 해시만 저장한다.
    private void saveRefreshToken(Long userId, String rawRefreshToken) {
        String tokenHash = tokenHashProvider.hash(rawRefreshToken);
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(jwtProperties.getRefreshTokenValiditySeconds());
        refreshTokenRepository.save(RefreshToken.create(userId, tokenHash, expiresAt));
    }

    // URL-safe Base64로 인코딩된 32바이트 랜덤 토큰을 생성한다.
    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // 이메일 중복과 로그인 조회 기준 통일
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
