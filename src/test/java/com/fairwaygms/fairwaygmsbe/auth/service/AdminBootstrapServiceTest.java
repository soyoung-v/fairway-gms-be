package com.fairwaygms.fairwaygmsbe.auth.service;

import com.fairwaygms.fairwaygmsbe.auth.config.AdminBootstrapProperties;
import com.fairwaygms.fairwaygmsbe.auth.domain.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.UserStatus;
import com.fairwaygms.fairwaygmsbe.auth.repository.UserRepository;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminBootstrapProperties properties;
    private AdminBootstrapService adminBootstrapService;

    @BeforeEach
    void setUp() {
        properties = new AdminBootstrapProperties();
        adminBootstrapService = new AdminBootstrapService(
                properties,
                userRepository,
                passwordEncoder,
                new PasswordPolicyValidator(),
                new MockEnvironment()
        );
    }

    @Test
    void bootstrapCreatesInitialAdmin() {
        // given
        properties.setEnabled(true);
        properties.setEmail("ADMIN@Test.com");
        properties.setPassword("password123!");
        properties.setName("관리자");
        when(userRepository.existsByRoleAndStatusAndIsDeletedFalse(UserRole.ADMIN, UserStatus.ACTIVE))
                .thenReturn(false);
        when(userRepository.findByEmailAndIsDeletedFalse("admin@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123!")).thenReturn("encoded-password");

        // when
        adminBootstrapService.bootstrap();

        // then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User admin = captor.getValue();
        assertThat(admin.getEmail()).isEqualTo("admin@test.com");
        assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(admin.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(admin.getGolfCourseId()).isNull();
        assertThat(admin.getEmailVerified()).isTrue();
        assertThat(admin.getPasswordHash()).isEqualTo("encoded-password");
    }

    @Test
    void bootstrapSkipsWhenActiveAdminExists() {
        // given
        properties.setEnabled(true);
        when(userRepository.existsByRoleAndStatusAndIsDeletedFalse(UserRole.ADMIN, UserStatus.ACTIVE))
                .thenReturn(true);

        // when
        adminBootstrapService.bootstrap();

        // then
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void bootstrapThrowsWhenSameEmailNonAdminExists() {
        // given
        properties.setEnabled(true);
        properties.setEmail("admin@test.com");
        properties.setPassword("password123!");
        User existingUser = User.createEmailUser(
                "admin@test.com",
                "encoded-password",
                "기존 사용자",
                null,
                UserRole.MANAGER,
                1L
        );
        when(userRepository.existsByRoleAndStatusAndIsDeletedFalse(UserRole.ADMIN, UserStatus.ACTIVE))
                .thenReturn(false);
        when(userRepository.findByEmailAndIsDeletedFalse("admin@test.com")).thenReturn(Optional.of(existingUser));

        // when & then
        assertThatThrownBy(() -> adminBootstrapService.bootstrap())
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ADMIN_BOOTSTRAP_REQUIRED));
    }
}
