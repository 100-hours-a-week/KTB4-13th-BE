package com.book.common.exception;

public enum CommonErrorCode implements ErrorCode {
    INVALID_REQUEST("INVALID_REQUEST", "요청 형식이 올바르지 않습니다."),
    STORAGE_FAILURE("STORAGE_FAILURE", "저장소 작업을 완료할 수 없습니다."),
    INTERNAL_ERROR("INTERNAL_ERROR", "알 수 없는 오류가 발생했습니다.");

    private final String code;
    private final String message;

    CommonErrorCode(final String code, final String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public int statusCode() {
        return switch (this) {
            case STORAGE_FAILURE, INTERNAL_ERROR -> 500;
            case INVALID_REQUEST -> 400;
        };
    }
}
