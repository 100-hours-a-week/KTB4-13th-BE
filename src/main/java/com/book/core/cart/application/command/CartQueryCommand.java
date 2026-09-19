package com.book.core.cart.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;

public record CartQueryCommand(Long userId) {
    public CartQueryCommand {
        if (userId == null || userId <= 0) {
            throw new CoreException(ErrorCode.UNAUTHORIZED);
        }
    }
}
