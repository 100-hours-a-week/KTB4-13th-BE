package com.book.common.exception;

public class CoreException extends RuntimeException {
    private final ErrorType errorType;
    private final Object data;

    public CoreException(final ErrorType errorType) {
        this(errorType, null, null);
    }

    public CoreException(final ErrorType errorType, final Object data) {
        this(errorType, data, null);
    }

    public CoreException(final ErrorType errorType, final Throwable cause) {
        this(errorType, null, cause);
    }

    private CoreException(final ErrorType errorType, final Object data, final Throwable cause) {
        super(errorType.message(), cause);
        this.errorType = errorType;
        this.data = data;
    }

    public ErrorType errorType() {
        return errorType;
    }

    public Object data() {
        return data;
    }
}
