package com.book.core.address.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;

// @formatter:off
public record GetAddressesCommand(Long userId) {
    public GetAddressesCommand {
        if (userId == null || userId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    public static GetAddressesCommand of(final Long userId) {
        return new GetAddressesCommand(userId);
    }
}
// @formatter:on
