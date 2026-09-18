package com.book.common.exception;

import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

public enum ErrorType {
    DEFAULT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.E500, "알 수 없는 오류가 발생했습니다.", LogLevel.ERROR),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, ErrorCode.E400, "요청 형식이 올바르지 않습니다.", LogLevel.INFO),
    STORAGE_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.E500, "저장소 작업을 완료할 수 없습니다.", LogLevel.ERROR),
    INVALID_SAMPLE_NAME(HttpStatus.BAD_REQUEST, ErrorCode.E400, "이름은 앞뒤 공백을 제외하고 1자 이상 100자 이하여야 합니다.", LogLevel.INFO),
    INVALID_SAMPLE_ID(HttpStatus.BAD_REQUEST, ErrorCode.E400, "샘플 ID는 양수여야 합니다.", LogLevel.INFO),
    SAMPLE_NOT_FOUND(HttpStatus.NOT_FOUND, ErrorCode.E401, "샘플을 찾을 수 없습니다.", LogLevel.INFO);

    private final HttpStatus status;
    private final ErrorCode code;
    private final String message;
    private final LogLevel logLevel;

    ErrorType(final HttpStatus status, final ErrorCode code, final String message, final LogLevel logLevel) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.logLevel = logLevel;
    }

    public HttpStatus status() {
        return status;
    }

    public ErrorCode code() {
        return code;
    }

    public String message() {
        return message;
    }

    public LogLevel logLevel() {
        return logLevel;
    }
}
