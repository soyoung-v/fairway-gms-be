package com.fairwaygms.fairwaygmsbe.assignment.exception;

import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCodeSpec;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AssignmentErrorCode implements ErrorCodeSpec {

    ASSIGNMENT_NOT_FOUND("ASSIGNMENT_NOT_FOUND", "배정을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ASSIGNMENT_ALREADY_EXISTS("ASSIGNMENT_ALREADY_EXISTS", "해당 예약팀에 이미 활성 배정이 존재합니다.", HttpStatus.CONFLICT),

    INVALID_ASSIGNMENT_STATUS("INVALID_ASSIGNMENT_STATUS", "현재 배정 상태에서 허용되지 않는 작업입니다.", HttpStatus.BAD_REQUEST),

    // 지정 캐디 배정(잠금)은 Manager만 해제 가능 (FR-512)
    ASSIGNMENT_LOCKED("ASSIGNMENT_LOCKED", "지정 캐디 배정이 잠겨 있어 자동 재배정할 수 없습니다.", HttpStatus.CONFLICT),

    // 하프백: 캐디 1명이 최대 2팀까지 담당 가능 (FR-506)
    CADDIE_ASSIGNMENT_LIMIT_EXCEEDED("CADDIE_ASSIGNMENT_LIMIT_EXCEEDED",
            "캐디의 당일 최대 배정 건수를 초과했습니다.", HttpStatus.CONFLICT),

    DAILY_SCHEDULE_NOT_FOUND("DAILY_SCHEDULE_NOT_FOUND", "일별 배정표를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DAILY_SCHEDULE_ALREADY_EXISTS("DAILY_SCHEDULE_ALREADY_EXISTS", "해당 날짜의 배정표가 이미 존재합니다.", HttpStatus.CONFLICT),
    DAILY_SCHEDULE_ALREADY_CONFIRMED("DAILY_SCHEDULE_ALREADY_CONFIRMED", "이미 확정된 배정표입니다.", HttpStatus.CONFLICT),
    DAILY_SCHEDULE_NOT_CONFIRMED("DAILY_SCHEDULE_NOT_CONFIRMED", "배정표가 확정 상태가 아닙니다.", HttpStatus.BAD_REQUEST),

    CART_ASSIGNMENT_NOT_FOUND("CART_ASSIGNMENT_NOT_FOUND", "카트 배정을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CART_ALREADY_ASSIGNED("CART_ALREADY_ASSIGNED", "해당 카트는 동일 날짜·티타임에 이미 배정되어 있습니다.", HttpStatus.CONFLICT),

    // 자동배정 실행 중 대기열 데이터 부족
    CADDIE_QUEUE_EMPTY("CADDIE_QUEUE_EMPTY", "배정 가능한 캐디 대기열이 없습니다.", HttpStatus.UNPROCESSABLE_ENTITY),

    CART_NOT_FOUND("CART_NOT_FOUND", "카트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    AssignmentErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
