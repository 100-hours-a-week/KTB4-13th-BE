package com.book.core.cart.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorType;
import com.book.core.cart.domain.CartItem;

public record CartAddCommand(long userId, long productId, int quantity) {
    public CartAddCommand {
        if (userId <= 0 || productId <= 0) {
            throw new CoreException(ErrorType.INVALID_REQUEST);
        }
        CartItem.requireQuantity(quantity);
    }
}
