package com.fairwaygms.fairwaygmsbe.auth.service;

import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PasswordPolicyValidator {

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S+$");

    // 회원가입과 초기 ADMIN 부트스트랩에서 같은 비밀번호 정책을 사용한다.
    public void validate(String password) {
        if (password == null
                || password.length() < 8
                || password.length() > 30
                || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
    }
}
