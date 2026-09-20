package com.book.core.auth.domain.exception;

import com.book.common.exception.ErrorCode;

public enum AuthErrorCode implements ErrorCode {
    INVALID_AUTHORIZATION_CODE("인가 코드가 유효하지 않습니다.", Category.UNAUTHORIZED),
    INVALID_ID_TOKEN("ID Token이 유효하지 않습니다.", Category.UNAUTHORIZED),
    INVALID_ACCESS_TOKEN("Access Token이 유효하지 않습니다.", Category.UNAUTHORIZED),
    OAUTH_PROVIDER_UNAVAILABLE("외부 인증 서비스를 사용할 수 없습니다.", Category.EXTERNAL_SERVICE_ERROR),
    OAUTH_PROVIDER_CONFIGURATION_ERROR("외부 인증 서비스 설정이 올바르지 않습니다.", Category.INTERNAL_ERROR),
    OAUTH_TOKEN_EXCHANGE_FAILURE("외부 인증 토큰을 발급받을 수 없습니다.", Category.INTERNAL_ERROR),
    TOKEN_ISSUE_FAILURE("인증 토큰을 발급할 수 없습니다.", Category.INTERNAL_ERROR);

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
