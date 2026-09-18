package com.book.core.cart.domain.exception;

import com.book.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CartErrorCode implements ErrorCode {
    INVALID_REQUEST("E400", "요청이 올바르지 않습니다."),
    FORBIDDEN("E403", "올바르지 않은 접근입니다."),
    OPERATION_NOT_ALLOWED("E8000", "장바구니에 허용되지 않는 작업입니다."),
    STOCK_NOT_ENOUGH("E8001", "장바구니에 담을 상품의 재고가 충분하지 않습니다."),
    INTERNAL_ERROR("E500", "알 수 없는 오류가 발생했습니다.");

    private final String code;
    private final String message;

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public int statusCode() {
        return switch (this) {
            case FORBIDDEN -> 403;
            case INTERNAL_ERROR -> 500;
            default -> 400;
        };
    }
}
