package com.fairwaygms.fairwaygmsbe.caddie.exception;

import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCodeSpec;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CaddieErrorCode implements ErrorCodeSpec {

    CADDIE_NOT_FOUND("CADDIE_NOT_FOUND", "캐디를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_CADDIE_NUMBER("DUPLICATE_CADDIE_NUMBER", "이미 사용 중인 캐디 번호입니다.", HttpStatus.CONFLICT),
    WORK_PATTERN_NOT_FOUND("WORK_PATTERN_NOT_FOUND", "근무 패턴 정보가 없습니다.", HttpStatus.NOT_FOUND),
    DESIGNATED_CART_NOT_FOUND("DESIGNATED_CART_NOT_FOUND", "지정카트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DESIGNATED_CART_ALREADY_EXISTS("DESIGNATED_CART_ALREADY_EXISTS", "이미 등록된 지정카트입니다.", HttpStatus.CONFLICT),
    DAILY_STATUS_NOT_FOUND("DAILY_STATUS_NOT_FOUND", "일별 근무 상태를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    QUEUE_NOT_FOUND("QUEUE_NOT_FOUND", "순번 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_QUEUE_NUMBER("DUPLICATE_QUEUE_NUMBER", "이미 사용 중인 순번입니다.", HttpStatus.CONFLICT),
    // 수동 조정 시 사유 없이 요청한 경우
    QUEUE_ADJUST_REASON_REQUIRED("QUEUE_ADJUST_REASON_REQUIRED", "순번 수동 조정 시 사유를 입력해야 합니다.", HttpStatus.BAD_REQUEST),
    CADDIE_ALREADY_LINKED("CADDIE_ALREADY_LINKED", "이미 계정과 연동된 캐디입니다.", HttpStatus.CONFLICT),
    INVALID_CADDIE_STATUS("INVALID_CADDIE_STATUS", "유효하지 않은 캐디 상태입니다.", HttpStatus.BAD_REQUEST),
    // DUTY 타입 등록 시 우선순위(FIRST/SECOND) 누락
    DUTY_PRIORITY_REQUIRED("DUTY_PRIORITY_REQUIRED", "당번 등록 시 우선순위(FIRST/SECOND)를 입력해야 합니다.", HttpStatus.BAD_REQUEST),
    CADDIE_GROUP_NOT_FOUND("CADDIE_GROUP_NOT_FOUND", "캐디 그룹을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CADDIE_GROUP_HAS_CADDIES("CADDIE_GROUP_HAS_CADDIES", "소속 캐디가 있는 그룹은 삭제할 수 없습니다. 캐디를 다른 그룹으로 이동한 뒤 삭제하세요.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    CaddieErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
