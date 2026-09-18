package com.book.core.cart.application.command;

import com.book.core.cart.domain.CartItem;

public record CartChangeQuantityCommand(long userId, long cartItemId, int quantity) {
    public CartChangeQuantityCommand {
        CartItem.requireId(userId);
        CartItem.requireId(cartItemId);
        CartItem.requireChangeQuantity(quantity);
    }
}
