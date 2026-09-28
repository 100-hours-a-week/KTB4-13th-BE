package com.book.common.exception;

public class BusinessException extends CoreException {
    public BusinessException(final ErrorCode errorCode) {
        super(errorCode);
    }

    public BusinessException(final ErrorCode errorCode, final Throwable cause) {
        super(errorCode, cause);
    }
}
