package com.book.core.auth.domain.exception;

import com.book.common.exception.ErrorCode;

public enum AuthErrorCode implements ErrorCode {
    INVALID_ID_TOKEN("ID Token이 유효하지 않습니다.", Category.UNAUTHORIZED),
    OAUTH_PROVIDER_UNAVAILABLE("외부 인증 서비스를 사용할 수 없습니다.", Category.EXTERNAL_SERVICE_ERROR);

    private final String message;
    private final Category category;

    AuthErrorCode(final String message, final Category category) {
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
