package com.book.core.user.domain.exception;

import com.book.common.exception.ErrorCode;

public enum UserErrorCode implements ErrorCode {
    INVALID_USER_ID("회원 ID는 양수여야 합니다.", Category.INVALID_INPUT),
    INVALID_NICKNAME("닉네임은 앞뒤 공백을 제외하고 2자 이상 20자 이하여야 합니다.", Category.INVALID_INPUT),
    INVALID_USER_PROVIDER_ID("회원 연동 ID는 양수여야 합니다.", Category.INVALID_INPUT),
    INVALID_PROVIDER_TYPE("소셜 로그인 제공자 유형이 필요합니다.", Category.INVALID_INPUT),
    INVALID_PROVIDER_USER_ID("소셜 로그인 사용자 ID는 1자 이상 255자 이하여야 합니다.", Category.INVALID_INPUT),
    INVALID_PROVIDER_EMAIL("소셜 로그인 이메일은 254자 이하여야 합니다.", Category.INVALID_INPUT),
    NICKNAME_CONFLICT("동일한 활성 닉네임이 이미 존재합니다.", Category.CONFLICT),
    PROVIDER_IDENTITY_CONFLICT("동일한 활성 소셜 로그인 계정이 이미 존재합니다.", Category.CONFLICT),
    USER_PROVIDER_USER_NOT_FOUND("회원 연결의 내부 회원을 찾을 수 없습니다.", Category.INTERNAL_ERROR),
    USER_PROVIDER_USER_INACTIVE("회원 연결의 내부 회원이 비활성 상태입니다.", Category.INTERNAL_ERROR);

    private final String message;
    private final Category category;

    UserErrorCode(final String message, final Category category) {
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
