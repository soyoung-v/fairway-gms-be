package com.fairwaygms.fairwaygmsbe.auth.dto;

import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SignupRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void validPasswordsPassPolicy() {
        // given
        String[] passwords = {
                "password123!",
                "Password123!",
                "fairway2026@"
        };

        // when & then
        for (String password : passwords) {
            assertThat(validatePassword(password))
                    .as("password=%s", password)
                    .isEmpty();
        }
    }

    @Test
    void invalidPasswordsFailPolicy() {
        // given
        String[] passwords = {
                "password123",
                "password!",
                "12345678!",
                "pass 123!",
                "short1!"
        };

        // when & then
        for (String password : passwords) {
            assertThat(validatePassword(password))
                    .as("password=%s", password)
                    .isNotEmpty();
        }
    }

    private Set<ConstraintViolation<SignupRequest>> validatePassword(String password) {
        SignupRequest request = new SignupRequest(
                "manager@test.com",
                password,
                "테스트 매니저",
                "010-1234-5678",
                UserRole.MANAGER,
                1L
        );
        return validator.validate(request);
    }
}
