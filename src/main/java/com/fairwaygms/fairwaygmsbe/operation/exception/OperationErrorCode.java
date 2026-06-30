package com.fairwaygms.fairwaygmsbe.operation.exception;

import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCodeSpec;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum OperationErrorCode implements ErrorCodeSpec {

    SETTING_NOT_FOUND("SETTING_NOT_FOUND", "운영 설정을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    SETTING_ALREADY_EXISTS("SETTING_ALREADY_EXISTS", "해당 월의 운영 설정이 이미 존재합니다.", HttpStatus.CONFLICT),
    PERIOD_NOT_FOUND("PERIOD_NOT_FOUND", "운영 부 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    TEE_TIME_NOT_FOUND("TEE_TIME_NOT_FOUND", "티타임을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_TEE_TIME("DUPLICATE_TEE_TIME", "동일 날짜/코스/시간에 티타임이 이미 존재합니다.", HttpStatus.CONFLICT),
    TEAM_NOT_FOUND("TEAM_NOT_FOUND", "예약팀을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_TEAM_STATUS("INVALID_TEAM_STATUS", "유효하지 않은 예약팀 상태입니다.", HttpStatus.BAD_REQUEST),
    SPECIAL_DAY_ALREADY_EXISTS("SPECIAL_DAY_ALREADY_EXISTS", "해당 날짜의 특별 운영일이 이미 존재합니다.", HttpStatus.CONFLICT),
    RAIN_POLICY_NOT_FOUND("RAIN_POLICY_NOT_FOUND", "우천취소 정책을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_YEAR_MONTH("INVALID_YEAR_MONTH", "유효하지 않은 연월 형식입니다. (예: 2025-06)", HttpStatus.BAD_REQUEST),
    // 티타임 일괄 재생성 시 기존 예약팀이 있는 경우
    TEE_TIME_HAS_TEAMS("TEE_TIME_HAS_TEAMS", "예약팀이 존재하는 티타임은 삭제할 수 없습니다.", HttpStatus.CONFLICT),
    CADDIE_NOT_FOUND("CADDIE_NOT_FOUND", "캐디를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_FILE_FORMAT("INVALID_FILE_FORMAT", "유효하지 않은 파일 형식입니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    OperationErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
