package com.book.common.exception;

public final class ErrorMessage {
    private final String code;
    private final String message;
    private final Object data;

    private ErrorMessage(final String code, final String message, final Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public ErrorMessage(final ErrorType errorType) {
        this(errorType, null);
    }

    public ErrorMessage(final ErrorType errorType, final Object data) {
        this(errorType.code().name(), errorType.message(), data);
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
