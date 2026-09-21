package com.book.core.address.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;

public record GetAddressesCommand(Long userId) {
    public GetAddressesCommand {
        if (userId == null || userId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
