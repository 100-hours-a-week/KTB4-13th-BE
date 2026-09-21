package com.book.core.cart.api.converter;

import com.book.core.cart.api.request.AddCartItemRequest;
import com.book.core.cart.api.request.DeleteCartItemsRequest;
import com.book.core.cart.api.request.ModifyCartItemRequest;
import com.book.core.cart.application.command.AddCartItemCommand;
import com.book.core.cart.application.command.DeleteCartItemCommand;
import com.book.core.cart.application.command.DeleteCartItemsCommand;
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

    public DeleteCartItemCommand toDeleteCartItemCommand(final Long userId, final Long cartItemId) {
        return new DeleteCartItemCommand(userId, cartItemId);
    }

    public DeleteCartItemsCommand toDeleteCartItemsCommand(final Long userId, final DeleteCartItemsRequest request) {
        return new DeleteCartItemsCommand(userId, request.cartItemIds());
    }

    public ModifyCartItemCommand toModifyCartItemCommand(
            final Long userId, final Long cartItemId, final ModifyCartItemRequest request) {
        return new ModifyCartItemCommand(userId, cartItemId, request.quantity());
    }
}
