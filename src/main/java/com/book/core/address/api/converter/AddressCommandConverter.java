package com.book.core.address.api.converter;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.address.api.request.RegisterAddressRequest;
import com.book.core.address.application.command.ListAddressCommand;
import com.book.core.address.application.command.RegisterAddressCommand;
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

    public ListAddressCommand toListAddressCommand(final String authorization, final Principal principal) {
        if (!hasBearerToken(authorization) || principal == null) {
            throw new CoreException(ErrorCode.UNAUTHORIZED);
        }

        try {
            final long userId = Long.parseLong(principal.getName());
            if (userId <= 0) {
                throw new CoreException(ErrorCode.UNAUTHORIZED);
            }
            return new ListAddressCommand(userId);
        } catch (final NumberFormatException exception) {
            throw new CoreException(ErrorCode.UNAUTHORIZED);
        }
    }

    private boolean hasBearerToken(final String authorization) {
        return authorization != null
                && authorization.startsWith("Bearer ")
                && !authorization.substring("Bearer ".length()).isBlank();
    }
}
