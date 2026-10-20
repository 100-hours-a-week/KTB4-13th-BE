package com.book.core.order.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;

public record ConfirmOrderItemCommand(Long userId, String orderKey, Long orderItemId) {
    public ConfirmOrderItemCommand {
        if (userId == null || userId <= 0 || orderKey == null || orderKey.isBlank() || orderItemId == null || orderItemId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
