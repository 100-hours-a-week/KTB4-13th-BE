package com.book.common.exception;

public interface ErrorCode {
    String code();

    String message();

    Category category();

    enum Category {
        INVALID_INPUT,
        UNAUTHORIZED,
        NOT_FOUND,
        CONFLICT,
        EXTERNAL_SERVICE_ERROR,
        INTERNAL_ERROR
    }
}
