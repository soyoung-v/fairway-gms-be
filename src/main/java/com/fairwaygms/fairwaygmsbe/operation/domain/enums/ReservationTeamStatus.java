package com.fairwaygms.fairwaygmsbe.operation.domain.enums;

public enum ReservationTeamStatus {
    RESERVED("예약"),
    CANCELLED("취소"),
    NO_SHOW("노쇼"),
    RAIN_CANCELLED("우천취소"),
    COMPLETED("완료");

    private final String label;

    ReservationTeamStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
