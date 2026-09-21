package com.book.core.auth.domain.exception;

import com.book.common.exception.ErrorCode;

public final class AuthErrorCode {
    public static final ErrorCode INVALID_AUTHORIZATION_CODE = ErrorCode.INVALID_AUTHORIZATION_CODE;
    public static final ErrorCode INVALID_ID_TOKEN = ErrorCode.INVALID_ID_TOKEN;
    public static final ErrorCode INVALID_ACCESS_TOKEN = ErrorCode.INVALID_ACCESS_TOKEN;
    public static final ErrorCode OAUTH_PROVIDER_UNAVAILABLE = ErrorCode.OAUTH_PROVIDER_UNAVAILABLE;
    public static final ErrorCode OAUTH_PROVIDER_CONFIGURATION_ERROR = ErrorCode.OAUTH_PROVIDER_CONFIGURATION_ERROR;
    public static final ErrorCode OAUTH_TOKEN_EXCHANGE_FAILURE = ErrorCode.OAUTH_TOKEN_EXCHANGE_FAILURE;
    public static final ErrorCode TOKEN_ISSUE_FAILURE = ErrorCode.TOKEN_ISSUE_FAILURE;

    private AuthErrorCode() {}
}
