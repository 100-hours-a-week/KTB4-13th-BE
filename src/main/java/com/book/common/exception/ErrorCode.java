package com.book.common.exception;

public interface ErrorCode {
    String code();

    String message();

    Category category();

    enum Category {
        INVALID_INPUT,
        NOT_FOUND,
        CONFLICT,
        INTERNAL_ERROR
    }
}
