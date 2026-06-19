package com.fairwaygms.fairwaygmsbe.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "토큰은 필수입니다.")
        String token,

        @NotBlank
        @Size(min = 8, max = 30, message = "비밀번호는 8자 이상 30자 이하여야 합니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S+$",
                message = "비밀번호는 영문, 숫자, 특수문자를 각각 1개 이상 포함하고 공백 없이 입력해주세요."
        )
        String newPassword
) {
}
