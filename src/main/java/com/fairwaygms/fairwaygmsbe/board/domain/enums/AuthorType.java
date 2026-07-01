package com.fairwaygms.fairwaygmsbe.board.domain.enums;

import lombok.Getter;

@Getter
public enum AuthorType {
    MANAGER("매니저"),
    CADDY("캐디");

    private final String label;

    AuthorType(String label) {
        this.label = label;
    }
}
