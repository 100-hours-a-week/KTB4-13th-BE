package com.book.core.address.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;

public record RegisterAddressCommand(
        Long userId, String label, String postalCode, String address, String detailAddress, boolean isDefault) {
    public RegisterAddressCommand {
        if (userId == null || userId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
