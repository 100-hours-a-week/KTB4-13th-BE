package com.book.common.exception;

public final class ErrorResponse {
    private final String code;
    private final String message;
    private final Object data;

    private ErrorResponse(final String code, final String message, final Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public ErrorResponse(final ErrorCode errorCode) {
        this(errorCode, null);
    }

    public ErrorResponse(final ErrorCode errorCode, final Object data) {
        this(errorCode.code(), errorCode.message(), data);
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }

    public Object data() {
        return data;
    }
}
