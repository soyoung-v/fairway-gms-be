package com.fairwaygms.fairwaygmsbe.auth.service;

import com.fairwaygms.fairwaygmsbe.auth.domain.RefreshToken;
import com.fairwaygms.fairwaygmsbe.auth.domain.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.UserStatus;
import com.fairwaygms.fairwaygmsbe.auth.dto.ChangePasswordRequest;
import com.fairwaygms.fairwaygmsbe.auth.dto.LoginRequest;
import com.fairwaygms.fairwaygmsbe.auth.dto.MeResponse;
import com.fairwaygms.fairwaygmsbe.auth.dto.SignupRequest;
import com.fairwaygms.fairwaygmsbe.auth.repository.RefreshTokenRepository;
import com.fairwaygms.fairwaygmsbe.auth.repository.UserRepository;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.JwtProperties;
import com.fairwaygms.fairwaygmsbe.common.security.JwtTokenProvider;
import com.fairwaygms.fairwaygmsbe.common.security.TokenHashProvider;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenHashProvider tokenHashProvider;

    private PasswordPolicyValidator passwordPolicyValidator;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setRefreshTokenValiditySeconds(1209600);
        passwordPolicyValidator = new PasswordPolicyValidator();
        authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtTokenProvider,
                jwtProperties,
                tokenHashProvider,
                passwordPolicyValidator
        );
    }

    @Test
    void signupStoresPasswordHash() {
        // given
        SignupRequest request = new SignupRequest(
                "MANAGER@Test.com",
                "password123!",
                "테스트 매니저",
                "010-1234-5678",
                UserRole.MANAGER,
                1L
        );
        when(userRepository.existsByEmailAndIsDeletedFalse("manager@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123!")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        authService.signup(request);

        // then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("manager@test.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(savedUser.getEmailVerified()).isFalse();
    }

    @Test
    void signupThrowsWhenEmailDuplicated() {
        // given
        SignupRequest request = new SignupRequest(
                "manager@test.com",
                "password123!",
                "테스트 매니저",
                null,
                UserRole.MANAGER,
                1L
        );
        when(userRepository.existsByEmailAndIsDeletedFalse("manager@test.com")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMAIL_DUPLICATED));
    }

    @Test
    void loginCreatesTokensAndStoresRefreshTokenHash() {
        // given
        User user = activeUser();
        when(userRepository.findByEmailAndIsDeletedFalse("manager@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123!", "encoded-password")).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(1L, UserRole.MANAGER, 10L)).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(1L, UserRole.MANAGER, 10L)).thenReturn("refresh-token");
        when(tokenHashProvider.hash("refresh-token")).thenReturn("refresh-hash");

        // when
        AuthLoginResult result = authService.login(new LoginRequest("manager@test.com", "password123!"));

        // then
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.user().userId()).isEqualTo(1L);
        assertThat(user.getLastLoginAt()).isNotNull();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(1L);
        assertThat(captor.getValue().getTokenHash()).isEqualTo("refresh-hash");
        assertThat(captor.getValue().getExpiresAt()).isNotNull();
    }

    @Test
    void loginThrowsWhenPasswordInvalid() {
        // given
        User user = activeUser();
        when(userRepository.findByEmailAndIsDeletedFalse("manager@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(new LoginRequest("manager@test.com", "wrong")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    void logoutRevokesRefreshToken() {
        // given
        RefreshToken refreshToken = RefreshToken.create(1L, "refresh-hash", java.time.LocalDateTime.now().plusDays(14));
        when(tokenHashProvider.hash("refresh-token")).thenReturn("refresh-hash");
        when(refreshTokenRepository.findByTokenHashAndIsDeletedFalse("refresh-hash"))
                .thenReturn(Optional.of(refreshToken));

        // when
        authService.logout("refresh-token");

        // then
        assertThat(refreshToken.getIsRevoked()).isTrue();
    }

    @Test
    void refreshRotatesRefreshToken() {
        // given
        User user = activeUser();
        RefreshToken storedToken = RefreshToken.create(1L, "old-refresh-hash",
                java.time.LocalDateTime.now().plusDays(14));
        when(jwtTokenProvider.validateToken("old-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken("old-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.getUserId("old-refresh-token")).thenReturn(1L);
        when(tokenHashProvider.hash("old-refresh-token")).thenReturn("old-refresh-hash");
        when(refreshTokenRepository.findByTokenHashAndIsDeletedFalse("old-refresh-hash"))
                .thenReturn(Optional.of(storedToken));
        when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.createAccessToken(1L, UserRole.MANAGER, 10L)).thenReturn("new-access-token");
        when(jwtTokenProvider.createRefreshToken(1L, UserRole.MANAGER, 10L)).thenReturn("new-refresh-token");
        when(tokenHashProvider.hash("new-refresh-token")).thenReturn("new-refresh-hash");

        // when
        AuthLoginResult result = authService.refresh("old-refresh-token");

        // then
        assertThat(storedToken.getIsRevoked()).isTrue();
        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isEqualTo("new-refresh-hash");
    }

    @Test
    void refreshThrowsWhenRefreshTokenRevoked() {
        // given
        RefreshToken storedToken = RefreshToken.create(1L, "refresh-hash",
                java.time.LocalDateTime.now().plusDays(14));
        storedToken.revoke();
        when(jwtTokenProvider.validateToken("refresh-token")).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken("refresh-token")).thenReturn(true);
        when(tokenHashProvider.hash("refresh-token")).thenReturn("refresh-hash");
        when(refreshTokenRepository.findByTokenHashAndIsDeletedFalse("refresh-hash"))
                .thenReturn(Optional.of(storedToken));

        // when & then
        assertThatThrownBy(() -> authService.refresh("refresh-token"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REFRESH_TOKEN_REVOKED));
    }

    @Test
    void getMeReturnsDto() {
        // given
        User user = activeUser();
        when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));

        // when
        MeResponse response = authService.getMe(1L);

        // then
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("manager@test.com");
        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void checkEmailReturnsTrueWhenAvailable() {
        // given
        when(userRepository.existsByEmailAndIsDeletedFalse("new@test.com")).thenReturn(false);

        // when
        boolean available = authService.isEmailAvailable("new@test.com");

        // then
        assertThat(available).isTrue();
    }

    @Test
    void checkEmailReturnsFalseWhenDuplicated() {
        // given
        when(userRepository.existsByEmailAndIsDeletedFalse("manager@test.com")).thenReturn(true);

        // when
        boolean available = authService.isEmailAvailable("manager@test.com");

        // then
        assertThat(available).isFalse();
    }

    @Test
    void changePasswordSucceeds() {
        // given
        User user = activeUser();
        when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123!", "encoded-password")).thenReturn(true);
        when(passwordEncoder.encode("newPass456!")).thenReturn("new-encoded-password");

        // when
        authService.changePassword(1L, new ChangePasswordRequest("password123!", "newPass456!"));

        // then
        assertThat(user.getPasswordHash()).isEqualTo("new-encoded-password");
    }

    @Test
    void changePasswordThrowsWhenCurrentPasswordInvalid() {
        // given
        User user = activeUser();
        when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass!", "encoded-password")).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.changePassword(1L,
                new ChangePasswordRequest("wrongPass!", "newPass456!")))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    void changePasswordThrowsWhenNewPasswordViolatesPolicy() {
        // given
        User user = activeUser();
        when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123!", "encoded-password")).thenReturn(true);

        // when & then — 특수문자 없는 비밀번호
        assertThatThrownBy(() -> authService.changePassword(1L,
                new ChangePasswordRequest("password123!", "onlyletters1")))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_PASSWORD));
    }

    private User activeUser() {
        User user = User.createEmailUser(
                "manager@test.com",
                "encoded-password",
                "테스트 매니저",
                "010-1234-5678",
                UserRole.MANAGER,
                10L
        );
        ReflectionTestUtils.setField(user, "id", 1L);
        user.approve(99L);
        return user;
    }
}
