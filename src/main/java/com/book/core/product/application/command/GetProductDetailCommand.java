package com.book.core.product.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;

public record GetProductDetailCommand(Long productId) {
    public GetProductDetailCommand {
        if (productId == null || productId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
