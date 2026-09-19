package com.book.core.user.domain.exception;

import com.book.common.exception.ErrorCode;

public enum UserErrorCode implements ErrorCode {
    INVALID_USER_ID("회원 ID는 양수여야 합니다."),
    INVALID_NICKNAME("닉네임은 앞뒤 공백을 제외하고 2자 이상 20자 이하여야 합니다."),
    INVALID_USER_PROVIDER_ID("회원 연동 ID는 양수여야 합니다."),
    INVALID_PROVIDER_TYPE("소셜 로그인 제공자 유형이 필요합니다."),
    INVALID_PROVIDER_USER_ID("소셜 로그인 사용자 ID는 1자 이상 255자 이하여야 합니다."),
    INVALID_PROVIDER_EMAIL("소셜 로그인 이메일은 254자 이하여야 합니다.");

    private final String message;

    UserErrorCode(final String message) {
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
    public Category category() {
        return Category.INVALID_INPUT;
    }
}
