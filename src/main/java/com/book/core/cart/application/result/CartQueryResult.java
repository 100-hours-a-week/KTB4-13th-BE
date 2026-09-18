package com.book.core.cart.application.result;

import java.util.List;

public record CartQueryResult(List<CartItemResult> cartItems) {
    public CartQueryResult {
        cartItems = List.copyOf(cartItems);
    }
}
