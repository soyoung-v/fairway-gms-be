package com.fairwaygms.fairwaygmsbe.caddie.application.model.res;

import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieGroup;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.CaddieGroupAssignmentType;

public record CaddieGroupRes(
        Long groupId,
        String name,
        CaddieGroupAssignmentType assignmentType,
        String assignmentTypeLabel,
        long caddieCount
) {
    public static CaddieGroupRes from(CaddieGroup group, long caddieCount) {
        return new CaddieGroupRes(
                group.getId(),
                group.getName(),
                group.getAssignmentType(),
                group.getAssignmentType().getLabel(),
                caddieCount
        );
    }
}
