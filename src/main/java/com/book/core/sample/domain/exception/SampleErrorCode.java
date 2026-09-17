package com.book.core.sample.domain.exception;

import com.book.common.exception.ErrorCode;

public enum SampleErrorCode implements ErrorCode {
    INVALID_SAMPLE_NAME("이름은 앞뒤 공백을 제외하고 1자 이상 100자 이하여야 합니다.", Category.INVALID_INPUT),
    INVALID_SAMPLE_ID("샘플 ID는 양수여야 합니다.", Category.INVALID_INPUT),
    SAMPLE_NOT_FOUND("샘플을 찾을 수 없습니다.", Category.NOT_FOUND);

    private final String message;
    private final Category category;

    SampleErrorCode(final String message, final Category category) {
        this.message = message;
        this.category = category;
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
    public Category category() {
        return category;
    }
}
