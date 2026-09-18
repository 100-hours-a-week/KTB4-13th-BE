package com.book.core.cart.application.command;

import com.book.common.exception.BusinessException;
import com.book.core.cart.domain.CartItem;
import com.book.core.cart.domain.exception.CartErrorCode;
import java.util.List;

public record CartDeleteCommand(long userId, List<Long> cartItemIds) {
    public CartDeleteCommand {
        CartItem.requireId(userId);
        if (cartItemIds == null
                || cartItemIds.isEmpty()
                || cartItemIds.size() > 30
                || cartItemIds.stream().anyMatch(java.util.Objects::isNull)) {
            throw new BusinessException(CartErrorCode.INVALID_REQUEST);
        }
        cartItemIds.forEach(CartItem::requireId);
        cartItemIds = cartItemIds.stream().distinct().toList();
    }
}
