package com.book.core.address.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;

public record UpdateAddressCommand(
        Long userId,
        Long addressId,
        PatchField<String> label,
        PatchField<String> postalCode,
        PatchField<String> address,
        PatchField<String> detailAddress,
        PatchField<Boolean> defaultAddress) {
    public UpdateAddressCommand {
        if (userId == null || userId <= 0 || addressId == null || addressId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (defaultAddress.provided() && defaultAddress.value() == null) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
