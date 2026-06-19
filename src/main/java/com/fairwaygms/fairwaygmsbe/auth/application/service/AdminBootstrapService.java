package com.fairwaygms.fairwaygmsbe.auth.application.service;

import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.enums.UserStatus;
import com.fairwaygms.fairwaygmsbe.auth.domain.repository.UserRepository;
import com.fairwaygms.fairwaygmsbe.auth.exception.AuthErrorCode;
import com.fairwaygms.fairwaygmsbe.auth.infrastructure.AdminBootstrapProperties;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminBootstrapService {

    private final AdminBootstrapProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final Environment environment;

    // 초기 ADMIN은 명시적으로 enabled=true일 때만 생성한다.
    @Transactional
    public void bootstrap() {
        if (userRepository.existsByRoleAndStatusAndIsDeletedFalse(UserRole.ADMIN, UserStatus.ACTIVE)) {
            return;
        }
        if (!properties.isEnabled()) {
            return;
        }

        validateRequiredSettings();
        String email = normalizeEmail(properties.getEmail());

        userRepository.findByEmailAndIsDeletedFalse(email)
                .ifPresent(user -> {
                    throw new BusinessException(AuthErrorCode.ADMIN_BOOTSTRAP_REQUIRED,
                            "초기 ADMIN email과 같은 계정이 이미 존재하지만 ACTIVE ADMIN이 아닙니다.");
                });

        passwordPolicyValidator.validate(properties.getPassword());
        User admin = User.createInitialAdmin(
                email,
                passwordEncoder.encode(properties.getPassword()),
                StringUtils.hasText(properties.getName()) ? properties.getName() : "관리자"
        );
        userRepository.save(admin);
    }

    // prod에서 enabled=true인 경우 email/password 누락을 기동 실패로 명확히 알린다.
    private void validateRequiredSettings() {
        if (StringUtils.hasText(properties.getEmail()) && StringUtils.hasText(properties.getPassword())) {
            return;
        }
        if (isProdProfile()) {
            throw new BusinessException(AuthErrorCode.ADMIN_BOOTSTRAP_REQUIRED);
        }
        throw new BusinessException(AuthErrorCode.ADMIN_BOOTSTRAP_REQUIRED,
                "초기 ADMIN email/password 설정이 필요합니다.");
    }

    private boolean isProdProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "prod".equalsIgnoreCase(profile));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
