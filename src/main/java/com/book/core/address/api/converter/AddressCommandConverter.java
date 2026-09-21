package com.book.core.address.api.converter;

import com.book.core.address.api.request.RegisterAddressRequest;
import com.book.core.address.api.request.UpdateAddressRequest;
import com.book.core.address.application.command.GetAddressesCommand;
import com.book.core.address.application.command.PatchField;
import com.book.core.address.application.command.RegisterAddressCommand;
import com.book.core.address.application.command.UpdateAddressCommand;
import java.security.Principal;
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

    public GetAddressesCommand toGetAddressesCommand(final Principal principal) {
        return new GetAddressesCommand(Long.parseLong(principal.getName()));
    }

    public UpdateAddressCommand toUpdateAddressCommand(
            final Long userId, final Long addressId, final UpdateAddressRequest request) {
        return new UpdateAddressCommand(
                userId,
                addressId,
                toPatchField(request.hasLabel(), request.label()),
                toPatchField(request.hasPostalCode(), request.postalCode()),
                toPatchField(request.hasAddress(), request.address()),
                toPatchField(request.hasDetailAddress(), request.detailAddress()),
                toPatchField(request.hasDefaultAddress(), request.defaultAddress()));
    }

    private static <T> PatchField<T> toPatchField(final boolean provided, final T value) {
        if (provided) {
            return PatchField.of(value);
        }
        return PatchField.absent();
    }
}
