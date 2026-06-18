package com.fairwaygms.fairwaygmsbe.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

// 모든 API 응답에 공통으로 사용하는 포맷. success / data / error 세 필드로 구성된다.
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL) // null 필드는 JSON 응답에서 제외한다 (e.g. 성공 시 error 필드 숨김)
public class ApiResponse<T> {

    // 요청 성공 여부 (항상 포함)
    private final boolean success;

    // 성공 시 반환할 데이터 (실패 시 null)
    private final T data;

    // 실패 시 반환할 에러 정보 (성공 시 null)
    private final ErrorResponse error;

    private ApiResponse(boolean success, T data, ErrorResponse error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    // 성공 응답 생성 — 반환할 데이터가 있는 경우 사용
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    // 성공 응답 생성 — 반환할 데이터가 없는 경우 (예: 삭제, 로그아웃)
    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, null, null);
    }

    // 실패 응답 생성 — 에러 코드와 메시지를 포함한다
    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorResponse(code, message));
    }

    // 에러 응답에 담기는 코드/메시지 쌍
    @Getter
    public static class ErrorResponse {

        // 에러 식별 코드 (예: "UNAUTHORIZED", "NOT_FOUND")
        private final String code;

        // 사용자에게 보여줄 에러 메시지
        private final String message;

        ErrorResponse(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
