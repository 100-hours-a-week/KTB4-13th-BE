package com.book.common.exception;

public class RetryAfterException extends RuntimeException {
    private final ErrorCode errorCode;
    private final long retryAfterSeconds;

    public RetryAfterException(final ErrorCode errorCode, final long retryAfterSeconds) {
        super(errorCode.message());
        this.errorCode = errorCode;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
