package com.book.core.cart.application.result;

import com.book.core.cart.domain.CartItem;

public record GetCartItemResult(Long cartItemId, Long productId, Integer quantity) {
    public static GetCartItemResult from(final CartItem item) {
        return new GetCartItemResult(item.id(), item.productId(), item.quantity());
    }
}
