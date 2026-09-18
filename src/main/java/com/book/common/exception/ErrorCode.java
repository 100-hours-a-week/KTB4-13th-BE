package com.book.common.exception;

public interface ErrorCode {
    String code();

    String message();

    default int statusCode() {
        return 400;
    }
}
