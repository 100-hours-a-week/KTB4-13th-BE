package com.book.core.cart.api.converter;

import com.book.core.cart.api.request.AddCartItemRequest;
import com.book.core.cart.application.command.AddCartItemCommand;
import com.book.core.cart.application.command.CartQueryCommand;
import org.springframework.stereotype.Component;

@Component
public class CartCommandConverter {
    public AddCartItemCommand toAddCartItemCommand(final Long userId, final AddCartItemRequest request) {
        return new AddCartItemCommand(userId, request.productId(), request.quantity());
    }

    public CartQueryCommand toCartQueryCommand(final Long userId) {
        return new CartQueryCommand(userId);
    }
}
