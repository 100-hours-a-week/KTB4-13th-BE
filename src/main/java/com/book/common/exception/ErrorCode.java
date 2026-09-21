package com.book.common.exception;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

@Getter
@Accessors(fluent = true)
public enum ErrorCode {
    DEFAULT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E500", "알 수 없는 오류가 발생했습니다.", LogLevel.ERROR),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "E400", "요청 형식이 올바르지 않습니다.", LogLevel.INFO),
    STORAGE_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_FAILURE", "저장소 작업을 완료할 수 없습니다.", LogLevel.ERROR),
    INVALID_USER_ID(HttpStatus.BAD_REQUEST, "INVALID_USER_ID", "회원 ID는 양수여야 합니다.", LogLevel.INFO),
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "INVALID_NICKNAME", "닉네임은 앞뒤 공백을 제외하고 2자 이상 20자 이하여야 합니다.", LogLevel.INFO),
    INVALID_USER_PROVIDER_ID(HttpStatus.BAD_REQUEST, "INVALID_USER_PROVIDER_ID", "회원 연동 ID는 양수여야 합니다.", LogLevel.INFO),
    INVALID_PROVIDER_TYPE(HttpStatus.BAD_REQUEST, "INVALID_PROVIDER_TYPE", "소셜 로그인 제공자 유형이 필요합니다.", LogLevel.INFO),
    INVALID_PROVIDER_USER_ID(
            HttpStatus.BAD_REQUEST, "INVALID_PROVIDER_USER_ID", "소셜 로그인 사용자 ID는 1자 이상 255자 이하여야 합니다.", LogLevel.INFO),
    INVALID_PROVIDER_EMAIL(
            HttpStatus.BAD_REQUEST, "INVALID_PROVIDER_EMAIL", "소셜 로그인 이메일은 254자 이하여야 합니다.", LogLevel.INFO),
    NICKNAME_CONFLICT(HttpStatus.CONFLICT, "NICKNAME_CONFLICT", "동일한 활성 닉네임이 이미 존재합니다.", LogLevel.INFO),
    PROVIDER_IDENTITY_CONFLICT(
            HttpStatus.CONFLICT, "PROVIDER_IDENTITY_CONFLICT", "동일한 활성 소셜 로그인 계정이 이미 존재합니다.", LogLevel.INFO),
    USER_PROVIDER_USER_NOT_FOUND(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "USER_PROVIDER_USER_NOT_FOUND",
            "회원 연결의 내부 회원을 찾을 수 없습니다.",
            LogLevel.ERROR),
    USER_PROVIDER_USER_INACTIVE(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "USER_PROVIDER_USER_INACTIVE",
            "회원 연결의 내부 회원이 비활성 상태입니다.",
            LogLevel.ERROR),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "E401", "인증 정보가 유효하지 않습니다.", LogLevel.INFO),
    ADDRESS_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "E400", "활성 주소지는 최대 3개까지 등록할 수 있습니다.", LogLevel.INFO),
    DUPLICATE_ADDRESS(HttpStatus.BAD_REQUEST, "E400", "동일한 주소·상세주소·별칭이 이미 등록되어 있습니다.", LogLevel.INFO),
    CART_ITEM_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "E8000", "장바구니에 담을 수 있는 상품 수를 초과했습니다.", LogLevel.INFO),
    INVALID_CART_ITEM_QUANTITY(HttpStatus.BAD_REQUEST, "E400", "장바구니 상품 수량은 1 이상 500 이하여야 합니다.", LogLevel.INFO),
    CART_NOT_FOUND(HttpStatus.NOT_FOUND, "E401", "장바구니를 찾을 수 없습니다.", LogLevel.INFO),
    INVALID_SAMPLE_NAME(HttpStatus.BAD_REQUEST, "E400", "이름은 앞뒤 공백을 제외하고 1자 이상 100자 이하여야 합니다.", LogLevel.INFO),
    INVALID_SAMPLE_ID(HttpStatus.BAD_REQUEST, "E400", "샘플 ID는 양수여야 합니다.", LogLevel.INFO),
    SAMPLE_NOT_FOUND(HttpStatus.NOT_FOUND, "E401", "샘플을 찾을 수 없습니다.", LogLevel.INFO);

    private final HttpStatus status;
    private final String code;
    private final String message;
    private final LogLevel logLevel;

    ErrorCode(final HttpStatus status, final String code, final String message, final LogLevel logLevel) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.logLevel = logLevel;
    }

    public Category category() {
        return switch (status) {
            case BAD_REQUEST -> Category.INVALID_INPUT;
            case UNAUTHORIZED -> Category.UNAUTHORIZED;
            case NOT_FOUND -> Category.NOT_FOUND;
            case CONFLICT -> Category.CONFLICT;
            case BAD_GATEWAY -> Category.EXTERNAL_SERVICE_ERROR;
            default -> Category.INTERNAL_ERROR;
        };
    }

    public enum Category {
        INVALID_INPUT,
        UNAUTHORIZED,
        NOT_FOUND,
        CONFLICT,
        EXTERNAL_SERVICE_ERROR,
        INTERNAL_ERROR
    }
}
