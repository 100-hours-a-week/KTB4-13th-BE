package com.book.common.exception;

public class CoreException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Object data;

    public CoreException(final ErrorCode errorCode) {
        this(errorCode, null, null);
    }

    public CoreException(final ErrorCode errorCode, final Object data) {
        this(errorCode, data, null);
    }

    public CoreException(final ErrorCode errorCode, final Throwable cause) {
        this(errorCode, null, cause);
    }

    private CoreException(final ErrorCode errorCode, final Object data, final Throwable cause) {
        super(errorCode.message(), cause);
        this.errorCode = errorCode;
        this.data = data;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public Object data() {
        return data;
    }
}
