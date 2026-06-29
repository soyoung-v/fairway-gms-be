package com.fairwaygms.fairwaygmsbe.auth.application.model.req;

import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupReq(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 30, message = "비밀번호는 8자 이상 30자 이하로 입력해주세요.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S+$",
                message = "비밀번호는 영문, 숫자, 특수문자를 각각 1개 이상 포함하고 공백 없이 입력해주세요."
        )
        String password,

        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        String name,

        @Size(max = 20, message = "연락처는 20자 이하여야 합니다.")
        String phone,

        @NotNull(message = "역할은 필수입니다.")
        UserRole role,

        Long golfCourseId
) {
}
