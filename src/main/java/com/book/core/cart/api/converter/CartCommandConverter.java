package com.book.core.cart.api.converter;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cart.api.request.AddCartItemRequest;
import com.book.core.cart.application.command.AddCartItemCommand;
import com.book.core.cart.application.command.CartQueryCommand;
import org.springframework.stereotype.Component;

@Component
public class CartCommandConverter {
    public AddCartItemCommand toAddCartItemCommand(final Long userId, final AddCartItemRequest request) {
        return new AddCartItemCommand(userId, request.productId(), request.quantity());
    }

    public CartQueryCommand toCartQueryCommand(final String authorization, final String principalName) {
        if (authorization == null
                || !authorization.startsWith("Bearer ")
                || authorization.substring("Bearer ".length()).isBlank()
                || principalName == null) {
            throw new CoreException(ErrorCode.UNAUTHORIZED);
        }
        try {
            return new CartQueryCommand(Long.valueOf(principalName));
        } catch (final NumberFormatException exception) {
            throw new CoreException(ErrorCode.UNAUTHORIZED, exception);
        }
    }
}
