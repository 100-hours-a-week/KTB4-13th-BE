package com.book.core.address.api.converter;

import com.book.core.address.api.request.RegisterAddressRequest;
import com.book.core.address.application.command.RegisterAddressCommand;
import org.springframework.stereotype.Component;

@Component
public class AddressCommandConverter {
    public RegisterAddressCommand toRegisterAddressCommand(final Long userId, final RegisterAddressRequest request) {
        return new RegisterAddressCommand(
                userId,
                request.label(),
                request.postalCode(),
                request.address(),
                request.detailAddress(),
                request.isDefault());
    }
}
