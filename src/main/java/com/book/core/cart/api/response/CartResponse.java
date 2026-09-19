package com.book.core.cart.api.response;

import com.book.core.cart.application.result.GetCartResult;
import java.util.List;

public record CartResponse(List<CartItemResponse> items) {
    public static CartResponse from(final GetCartResult result) {
        return new CartResponse(
                result.items().stream().map(CartItemResponse::from).toList());
    }
}
