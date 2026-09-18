package com.book.core.cart.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorType;
import com.book.core.cart.domain.CartItem;

public record CartAddCommand(Long userId, Long productId, Integer quantity) {
    public CartAddCommand {
        if (userId == null || userId <= 0 || productId == null || productId <= 0 || quantity == null) {
            throw new CoreException(ErrorType.INVALID_REQUEST);
        }
        CartItem.requireQuantity(quantity);
    }
}
