package com.book.core.cart.api.response;

import com.book.core.cart.application.result.CartQueryResult;
import java.util.List;

public record CartResponse(List<CartItemResponse> items) {
    public static CartResponse from(final CartQueryResult result) {
        return new CartResponse(
                result.items().stream().map(CartItemResponse::from).toList());
    }
}
