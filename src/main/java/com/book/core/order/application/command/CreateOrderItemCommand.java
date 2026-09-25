package com.book.core.order.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.domain.OrderItem;

public record CreateOrderItemCommand(Long productId, Integer quantity) {
    public CreateOrderItemCommand {
        if (productId == null || productId <= 0 || quantity == null || !OrderItem.isQuantityInRange(quantity)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
