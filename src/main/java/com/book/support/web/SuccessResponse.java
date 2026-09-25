package com.book.support.web;

public record SuccessResponse<T>(String result, T data, ErrorResponse error) {
    private static final String SUCCESS = "SUCCESS";

    public static <T> SuccessResponse<T> of(final T data) {
        return new SuccessResponse<>(SUCCESS, data, null);
    }
}
