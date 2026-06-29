package com.fairwaygms.fairwaygmsbe.auth.application.service;

import com.fairwaygms.fairwaygmsbe.auth.application.model.res.AdminUserRes;
import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.enums.UserStatus;
import com.fairwaygms.fairwaygmsbe.auth.domain.repository.UserRepository;
import com.fairwaygms.fairwaygmsbe.auth.exception.AuthErrorCode;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserService(userRepository);
    }

    @Test
    void approvePendingUserSucceeds() {
        // given
        User pendingUser = pendingUser();
        when(userRepository.findByIdAndIsDeletedFalse(2L)).thenReturn(Optional.of(pendingUser));

        // when
        AdminUserRes response = adminUserService.approveUser(admin(), 2L);

        // then
        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(pendingUser.getApprovedBy()).isEqualTo(1L);
        assertThat(pendingUser.getApprovedAt()).isNotNull();
    }

    @Test
    void rejectActiveUserFails() {
        // given
        User activeUser = pendingUser();
        activeUser.approve(1L);
        when(userRepository.findByIdAndIsDeletedFalse(2L)).thenReturn(Optional.of(activeUser));

        // when & then
        assertThatThrownBy(() -> adminUserService.rejectUser(admin(), 2L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.ALREADY_PROCESSED));
    }

    @Test
    void managerCannotApproveUser() {
        // when & then
        assertThatThrownBy(() -> adminUserService.approveUser(manager(), 2L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    private AuthenticatedUser admin() {
        return new AuthenticatedUser(1L, UserRole.ADMIN, null);
    }

    private AuthenticatedUser manager() {
        return new AuthenticatedUser(3L, UserRole.MANAGER, 10L);
    }

    private User pendingUser() {
        User user = User.createEmailUser(
                "manager@test.com",
                "encoded-password",
                "테스트 매니저",
                "010-1234-5678",
                UserRole.MANAGER,
                10L
        );
        ReflectionTestUtils.setField(user, "id", 2L);
        return user;
    }
}
