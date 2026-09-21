package com.book.core.user.domain.exception;

import com.book.common.exception.ErrorCode;

public final class UserErrorCode {
    public static final ErrorCode INVALID_USER_ID = ErrorCode.INVALID_USER_ID;
    public static final ErrorCode INVALID_NICKNAME = ErrorCode.INVALID_NICKNAME;
    public static final ErrorCode INVALID_USER_PROVIDER_ID = ErrorCode.INVALID_USER_PROVIDER_ID;
    public static final ErrorCode INVALID_PROVIDER_TYPE = ErrorCode.INVALID_PROVIDER_TYPE;
    public static final ErrorCode INVALID_PROVIDER_USER_ID = ErrorCode.INVALID_PROVIDER_USER_ID;
    public static final ErrorCode INVALID_PROVIDER_EMAIL = ErrorCode.INVALID_PROVIDER_EMAIL;
    public static final ErrorCode NICKNAME_CONFLICT = ErrorCode.NICKNAME_CONFLICT;
    public static final ErrorCode PROVIDER_IDENTITY_CONFLICT = ErrorCode.PROVIDER_IDENTITY_CONFLICT;
    public static final ErrorCode USER_PROVIDER_USER_NOT_FOUND = ErrorCode.USER_PROVIDER_USER_NOT_FOUND;
    public static final ErrorCode USER_PROVIDER_USER_INACTIVE = ErrorCode.USER_PROVIDER_USER_INACTIVE;

    private UserErrorCode() {}
}
