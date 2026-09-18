package com.book.core.sample.domain.exception;

import com.book.common.exception.ErrorCode;

public enum SampleErrorCode implements ErrorCode {
    INVALID_SAMPLE_NAME("이름은 앞뒤 공백을 제외하고 1자 이상 100자 이하여야 합니다."),
    INVALID_SAMPLE_ID("샘플 ID는 양수여야 합니다."),
    SAMPLE_NOT_FOUND("샘플을 찾을 수 없습니다.");

    private final String message;

    SampleErrorCode(final String message) {
        this.message = message;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public int statusCode() {
        return switch (this) {
            case SAMPLE_NOT_FOUND -> 404;
            default -> 400;
        };
    }
}
