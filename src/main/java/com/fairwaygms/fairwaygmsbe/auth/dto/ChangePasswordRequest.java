package com.fairwaygms.fairwaygmsbe.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 비밀번호 변경 요청. 현재 비밀번호 검증 후 새 비밀번호로 교체한다.
public record ChangePasswordRequest(

        @NotBlank(message = "현재 비밀번호는 필수입니다.")
        String currentPassword,

        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @Size(min = 8, max = 30, message = "비밀번호는 8자 이상 30자 이하로 입력해주세요.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S+$",
                message = "비밀번호는 영문, 숫자, 특수문자를 각각 1개 이상 포함하고 공백 없이 입력해주세요."
        )
        String newPassword
) {
}
