package com.fairwaygms.fairwaygmsbe.auth.application.service;

import com.fairwaygms.fairwaygmsbe.auth.application.model.res.AdminUserRes;
import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.enums.UserStatus;
import com.fairwaygms.fairwaygmsbe.auth.domain.repository.UserRepository;
import com.fairwaygms.fairwaygmsbe.auth.exception.AuthErrorCode;
import com.fairwaygms.fairwaygmsbe.caddie.application.service.CaddieService;
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
    private final CaddieService caddieService;

    // 승인 대기 사용자는 오래된 가입순으로 반환한다.
    @Transactional(readOnly = true)
    public List<AdminUserRes> getPendingUsers(AuthenticatedUser admin) {
        validateAdmin(admin);
        return userRepository.findByStatusAndIsDeletedFalseOrderByCreatedAtAsc(UserStatus.PENDING)
                .stream()
                .map(AdminUserRes::from)
                .toList();
    }

    // PENDING 계정만 ACTIVE로 승인한다.
    @Transactional
    public AdminUserRes approveUser(AuthenticatedUser admin, Long userId) {
        validateAdmin(admin);
        User user = getUser(userId);
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BusinessException(AuthErrorCode.ALREADY_PROCESSED, "이미 승인된 사용자입니다.");
        }
        if (user.getStatus() != UserStatus.PENDING) {
            throw new BusinessException(AuthErrorCode.ALREADY_PROCESSED, "승인 대기 상태가 아닌 사용자입니다.");
        }
        user.approve(admin.getUserId());

        // CADDY 계정 승인 시 caddie 레코드 + 기본 근무패턴을 생성한다
        if (user.getRole() == UserRole.CADDY) {
            caddieService.createOnApproval(user);
        }

        return AdminUserRes.from(user);
    }

    // ACTIVE 계정은 거절할 수 없고, PENDING 계정만 REJECTED로 변경한다.
    @Transactional
    public AdminUserRes rejectUser(AuthenticatedUser admin, Long userId) {
        validateAdmin(admin);
        User user = getUser(userId);
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BusinessException(AuthErrorCode.ALREADY_PROCESSED, "활성 사용자는 거절할 수 없습니다.");
        }
        if (user.getStatus() != UserStatus.PENDING) {
            throw new BusinessException(AuthErrorCode.ALREADY_PROCESSED, "승인 대기 상태가 아닌 사용자입니다.");
        }
        user.reject(admin.getUserId());
        return AdminUserRes.from(user);
    }

    // ACTIVE Manager만 퇴사 처리할 수 있다.
    @Transactional
    public AdminUserRes withdrawManager(AuthenticatedUser admin, Long userId) {
        validateAdmin(admin);
        User user = getUser(userId);
        if (user.getRole() != UserRole.MANAGER) {
            throw new BusinessException(AuthErrorCode.INVALID_ROLE, "Manager 계정만 퇴사 처리할 수 있습니다.");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(AuthErrorCode.ALREADY_PROCESSED, "활성 상태의 Manager만 퇴사 처리할 수 있습니다.");
        }
        user.withdraw();
        return AdminUserRes.from(user);
    }

    // ADMIN 전용 — status 없으면 전체 목록 반환
    @Transactional(readOnly = true)
    public List<AdminUserRes> getUsers(AuthenticatedUser admin, UserStatus status) {
        validateAdmin(admin);
        List<User> users = status != null
                ? userRepository.findByStatusAndIsDeletedFalseOrderByCreatedAtAsc(status)
                : userRepository.findByIsDeletedFalseOrderByCreatedAtAsc();
        return users.stream().map(AdminUserRes::from).toList();
    }

    // Manager 전용 — 자기 골프장 소속 CADDY 승인 대기 목록
    @Transactional(readOnly = true)
    public List<AdminUserRes> getPendingCaddiesForManager(AuthenticatedUser manager) {
        validateManager(manager);
        return userRepository.findByRoleAndStatusAndGolfCourseIdAndIsDeletedFalseOrderByCreatedAtAsc(
                        UserRole.CADDY, UserStatus.PENDING, manager.getGolfCourseId())
                .stream()
                .map(AdminUserRes::from)
                .toList();
    }

    // Manager 전용 — 자기 골프장 소속 CADDY만 승인 가능
    @Transactional
    public AdminUserRes approveCaddieAsManager(AuthenticatedUser manager, Long userId) {
        validateManager(manager);
        User user = getUser(userId);
        if (user.getRole() != UserRole.CADDY) {
            throw new BusinessException(AuthErrorCode.INVALID_ROLE, "Manager는 CADDY 계정만 승인할 수 있습니다.");
        }
        if (!manager.getGolfCourseId().equals(user.getGolfCourseId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "소속 골프장의 캐디만 승인할 수 있습니다.");
        }
        if (user.getStatus() != UserStatus.PENDING) {
            throw new BusinessException(AuthErrorCode.ALREADY_PROCESSED, "승인 대기 상태가 아닌 사용자입니다.");
        }
        user.approve(manager.getUserId());
        caddieService.createOnApproval(user);
        return AdminUserRes.from(user);
    }

    // Manager 전용 — 자기 골프장 소속 CADDY만 거절 가능
    @Transactional
    public AdminUserRes rejectCaddieAsManager(AuthenticatedUser manager, Long userId) {
        validateManager(manager);
        User user = getUser(userId);
        if (user.getRole() != UserRole.CADDY) {
            throw new BusinessException(AuthErrorCode.INVALID_ROLE, "Manager는 CADDY 계정만 거절할 수 있습니다.");
        }
        if (!manager.getGolfCourseId().equals(user.getGolfCourseId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "소속 골프장의 캐디만 거절할 수 있습니다.");
        }
        if (user.getStatus() != UserStatus.PENDING) {
            throw new BusinessException(AuthErrorCode.ALREADY_PROCESSED, "승인 대기 상태가 아닌 사용자입니다.");
        }
        user.reject(manager.getUserId());
        return AdminUserRes.from(user);
    }

    // Method Security를 켜지 않고 Service 내부에서 ADMIN 권한을 검증한다.
    private void validateAdmin(AuthenticatedUser user) {
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateManager(AuthenticatedUser user) {
        if (user == null || user.getRole() != UserRole.MANAGER) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private User getUser(Long userId) {
        return userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
    }
}
