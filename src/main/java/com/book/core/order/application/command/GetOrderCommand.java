package com.book.core.order.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;

// @formatter:off
public record GetOrderCommand(Long userId, String orderKey) {
    public GetOrderCommand {
        if (userId == null || userId <= 0 || orderKey == null || orderKey.isBlank()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
// @formatter:on
