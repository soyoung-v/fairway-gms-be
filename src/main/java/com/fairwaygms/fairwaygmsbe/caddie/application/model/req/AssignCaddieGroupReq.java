package com.fairwaygms.fairwaygmsbe.caddie.application.model.req;

// groupId가 null이면 그룹 해제 — 해제된 캐디는 자동배정에서 HOUSE 그룹으로 취급된다.
public record AssignCaddieGroupReq(
        Long groupId
) {}
