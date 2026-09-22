package com.book.core.address.api.converter;

import com.book.core.address.api.request.RegisterAddressRequest;
import com.book.core.address.api.request.UpdateAddressRequest;
import com.book.core.address.application.command.GetAddressesCommand;
import com.book.core.address.application.command.RegisterAddressCommand;
import com.book.core.address.application.command.UpdateAddressCommand;
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

    public GetAddressesCommand toGetAddressesCommand(final Long userId) {
        return new GetAddressesCommand(userId);
    }

    public UpdateAddressCommand toUpdateAddressCommand(
            final Long userId, final Long addressId, final UpdateAddressRequest request) {
        return new UpdateAddressCommand(
                userId, addressId, request.label(), request.postalCode(), request.address(), request.detailAddress());
    }
}
