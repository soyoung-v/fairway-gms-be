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

class AuthEmailValidationTest {

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
    void signupEmailValidFormatPasses() {
        // when
        Set<ConstraintViolation<SignupRequest>> violations = validateSignupEmail("manager@test.com");

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void signupEmailWithoutAtFails() {
        // when
        Set<ConstraintViolation<SignupRequest>> violations = validateSignupEmail("manager-test.com");

        // then
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("올바른 이메일 형식이 아닙니다.");
    }

    @Test
    void signupEmailBlankFails() {
        // when
        Set<ConstraintViolation<SignupRequest>> violations = validateSignupEmail("");

        // then
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("이메일은 필수입니다.");
    }

    @Test
    void loginEmailValidFormatPasses() {
        // when
        Set<ConstraintViolation<LoginRequest>> violations = validateLoginEmail("manager@test.com");

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void loginEmailWithoutAtFails() {
        // when
        Set<ConstraintViolation<LoginRequest>> violations = validateLoginEmail("manager-test.com");

        // then
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("올바른 이메일 형식이 아닙니다.");
    }

    private Set<ConstraintViolation<SignupRequest>> validateSignupEmail(String email) {
        SignupRequest request = new SignupRequest(
                email,
                "password123!",
                "테스트 매니저",
                "010-1234-5678",
                UserRole.MANAGER,
                1L
        );
        return validator.validate(request);
    }

    private Set<ConstraintViolation<LoginRequest>> validateLoginEmail(String email) {
        LoginRequest request = new LoginRequest(email, "password123!");
        return validator.validate(request);
    }
}
