package com.fairwaygms.fairwaygmsbe.golfcourse.exception;

import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCodeSpec;
import lombok.Getter;
import org.springframework.http.HttpStatus;

// golfcourse 도메인 전용 에러코드
// GOLF_COURSE_NOT_FOUND는 common ErrorCode에 이미 정의되어 있으므로 여기서는 제외한다.
@Getter
public enum GolfCourseErrorCode implements ErrorCodeSpec {

    COURSE_NOT_FOUND("COURSE_NOT_FOUND", "코스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CART_NOT_FOUND("CART_NOT_FOUND", "카트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_CART_NUMBER("DUPLICATE_CART_NUMBER", "이미 등록된 카트 번호입니다.", HttpStatus.CONFLICT),
    COURSE_NAME_DUPLICATED("COURSE_NAME_DUPLICATED", "이미 등록된 코스명입니다.", HttpStatus.CONFLICT),
    INVALID_HOLE_COUNT("INVALID_HOLE_COUNT", "홀 수는 9, 18, 27 중 하나여야 합니다.", HttpStatus.BAD_REQUEST),
    INVALID_STATUS("INVALID_STATUS", "유효하지 않은 상태값입니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    GolfCourseErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
