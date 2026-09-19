package com.book.core.cart.api.response;

import com.book.core.cart.application.result.GetCartItemResult;

public record CartItemResponse(Long cartItemId, Long productId, Integer quantity) {
    public static CartItemResponse from(final GetCartItemResult result) {
        return new CartItemResponse(result.cartItemId(), result.productId(), result.quantity());
    }
}
