package com.book.core.cart.application.command;

import com.book.core.cart.domain.CartItem;

public record CartAddCommand(long userId, long productId, int quantity) {
    public CartAddCommand {
        CartItem.requireId(userId);
        CartItem.requireId(productId);
        CartItem.requireQuantity(quantity);
    }

    @Override
    public String toString() {
        return "CartAddCommand[userId=" + userId + ", productId=" + productId + ", quantity=" + quantity + "]";
    }
}
