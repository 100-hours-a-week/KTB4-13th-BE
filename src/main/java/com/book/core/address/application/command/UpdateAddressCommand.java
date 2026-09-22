package com.book.core.address.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;

public record UpdateAddressCommand(
        Long userId, Long addressId, String label, String postalCode, String address, String detailAddress) {
    public UpdateAddressCommand {
        if (userId == null || userId <= 0 || addressId == null || addressId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
