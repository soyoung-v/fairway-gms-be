package com.fairwaygms.fairwaygmsbe.settlement.exception;

import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCodeSpec;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum SettlementErrorCode implements ErrorCodeSpec {

    FEE_POLICY_NOT_FOUND("FEE_POLICY_NOT_FOUND", "캐디피 정책을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    ASSIGNMENT_RECORD_NOT_FOUND("ASSIGNMENT_RECORD_NOT_FOUND", "배정 기록을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ASSIGNMENT_RECORD_ALREADY_EXISTS("ASSIGNMENT_RECORD_ALREADY_EXISTS", "이미 처리된 배정 기록입니다.", HttpStatus.CONFLICT),

    SETTLEMENT_NOT_FOUND("SETTLEMENT_NOT_FOUND", "정산 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    SETTLEMENT_ALREADY_CONFIRMED("SETTLEMENT_ALREADY_CONFIRMED", "이미 확정된 정산입니다.", HttpStatus.CONFLICT),
    SETTLEMENT_NOT_CONFIRMED("SETTLEMENT_NOT_CONFIRMED", "확정되지 않은 정산입니다.", HttpStatus.BAD_REQUEST),

    CADDIE_SETTLEMENT_NOT_FOUND("CADDIE_SETTLEMENT_NOT_FOUND", "캐디 정산 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    ADJUSTMENT_REASON_REQUIRED("ADJUSTMENT_REASON_REQUIRED", "수동 조정 시 사유를 입력해야 합니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    SettlementErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
