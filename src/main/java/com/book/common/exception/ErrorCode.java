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
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "E401", "인증 정보가 유효하지 않습니다.", LogLevel.INFO),
    FORBIDDEN(HttpStatus.FORBIDDEN, "E403", "올바르지 않은 접근입니다.", LogLevel.INFO),
    ADDRESS_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "E400", "활성 주소지는 최대 3개까지 등록할 수 있습니다.", LogLevel.INFO),
    DUPLICATE_ADDRESS(HttpStatus.BAD_REQUEST, "E400", "동일한 주소·상세주소·별칭이 이미 등록되어 있습니다.", LogLevel.INFO),
    CART_ITEM_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "E8000", "장바구니에 담을 수 있는 상품 수를 초과했습니다.", LogLevel.INFO),
    INVALID_CART_ITEM_QUANTITY(HttpStatus.BAD_REQUEST, "E400", "장바구니 상품 수량은 1 이상 500 이하여야 합니다.", LogLevel.INFO),
    CART_NOT_FOUND(HttpStatus.NOT_FOUND, "E401", "장바구니를 찾을 수 없습니다.", LogLevel.INFO),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "E404", "상품을 찾을 수 없습니다.", LogLevel.INFO),
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
}
