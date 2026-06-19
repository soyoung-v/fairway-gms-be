package com.fairwaygms.fairwaygmsbe.auth.service;

import com.fairwaygms.fairwaygmsbe.auth.domain.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.UserStatus;
import com.fairwaygms.fairwaygmsbe.auth.dto.AdminUserResponse;
import com.fairwaygms.fairwaygmsbe.auth.repository.UserRepository;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    // 승인 대기 사용자는 오래된 가입순으로 반환한다.
    @Transactional(readOnly = true)
    public List<AdminUserResponse> getPendingUsers(AuthenticatedUser admin) {
        validateAdmin(admin);
        return userRepository.findByStatusAndIsDeletedFalseOrderByCreatedAtAsc(UserStatus.PENDING)
                .stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    // PENDING 계정만 ACTIVE로 승인한다.
    @Transactional
    public AdminUserResponse approveUser(AuthenticatedUser admin, Long userId) {
        validateAdmin(admin);
        User user = getUser(userId);
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ALREADY_PROCESSED, "이미 승인된 사용자입니다.");
        }
        if (user.getStatus() != UserStatus.PENDING) {
            throw new BusinessException(ErrorCode.ALREADY_PROCESSED, "승인 대기 상태가 아닌 사용자입니다.");
        }
        user.approve(admin.getUserId());
        return AdminUserResponse.from(user);
    }

    // ACTIVE 계정은 거절할 수 없고, PENDING 계정만 REJECTED로 변경한다.
    @Transactional
    public AdminUserResponse rejectUser(AuthenticatedUser admin, Long userId) {
        validateAdmin(admin);
        User user = getUser(userId);
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ALREADY_PROCESSED, "활성 사용자는 거절할 수 없습니다.");
        }
        if (user.getStatus() != UserStatus.PENDING) {
            throw new BusinessException(ErrorCode.ALREADY_PROCESSED, "승인 대기 상태가 아닌 사용자입니다.");
        }
        user.reject(admin.getUserId());
        return AdminUserResponse.from(user);
    }

    // Method Security를 켜지 않고 Service 내부에서 ADMIN 권한을 검증한다.
    private void validateAdmin(AuthenticatedUser user) {
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private User getUser(Long userId) {
        return userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
