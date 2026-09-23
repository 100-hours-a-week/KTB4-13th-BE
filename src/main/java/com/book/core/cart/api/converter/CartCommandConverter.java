package com.book.core.cart.api.converter;

import com.book.core.cart.api.request.AddCartItemRequest;
import com.book.core.cart.api.request.ModifyCartItemRequest;
import com.book.core.cart.application.command.AddCartItemCommand;
import com.book.core.cart.application.command.GetCartCommand;
import com.book.core.cart.application.command.ModifyCartItemCommand;
import org.springframework.stereotype.Component;

@Component
public class CartCommandConverter {
    public AddCartItemCommand toAddCartItemCommand(final Long userId, final AddCartItemRequest request) {
        return new AddCartItemCommand(userId, request.productId(), request.quantity());
    }

    public GetCartCommand toGetCartCommand(final Long userId) {
        return new GetCartCommand(userId);
    }

    public ModifyCartItemCommand toModifyCartItemCommand(
            final Long userId, final Long cartItemId, final ModifyCartItemRequest request) {
        return new ModifyCartItemCommand(userId, cartItemId, request.quantity());
    }
}
