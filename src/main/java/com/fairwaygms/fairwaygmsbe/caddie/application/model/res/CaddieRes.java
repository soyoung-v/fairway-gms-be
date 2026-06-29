package com.fairwaygms.fairwaygmsbe.caddie.application.model.res;

import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.CaddieStatus;

import java.time.LocalDate;

public record CaddieRes(
        Long id,
        Long golfCourseId,
        Long userId,
        String caddieNumber,
        String name,
        String phone,
        LocalDate hireDate,
        CaddieStatus status,
        String statusLabel
) {
    public static CaddieRes from(Caddie caddie) {
        return new CaddieRes(
                caddie.getId(),
                caddie.getGolfCourse().getId(),
                caddie.getUser() != null ? caddie.getUser().getId() : null,
                caddie.getCaddieNumber(),
                caddie.getName(),
                caddie.getPhone(),
                caddie.getHireDate(),
                caddie.getStatus(),
                caddie.getStatus().getLabel()
        );
    }
}
