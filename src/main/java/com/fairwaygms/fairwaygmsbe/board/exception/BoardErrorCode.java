package com.fairwaygms.fairwaygmsbe.board.exception;

import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCodeSpec;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum BoardErrorCode implements ErrorCodeSpec {

    POST_NOT_FOUND("POST_NOT_FOUND", "게시글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    POST_ACCESS_DENIED("POST_ACCESS_DENIED", "게시글에 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),

    COMMENT_NOT_FOUND("COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    COMMENT_ACCESS_DENIED("COMMENT_ACCESS_DENIED", "댓글에 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),

    SWAP_REQUEST_NOT_FOUND("SWAP_REQUEST_NOT_FOUND", "순번 교환 요청을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    SWAP_REQUEST_ALREADY_PROCESSED("SWAP_REQUEST_ALREADY_PROCESSED", "이미 처리된 순번 교환 요청입니다.", HttpStatus.CONFLICT),
    SELF_SWAP_NOT_ALLOWED("SELF_SWAP_NOT_ALLOWED", "자기 자신과의 순번 교환은 요청할 수 없습니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    BoardErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
