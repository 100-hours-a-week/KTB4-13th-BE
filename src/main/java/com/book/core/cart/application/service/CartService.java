package com.book.core.cart.application.service;

import com.book.core.cart.application.command.AddCartItemCommand;
import com.book.core.cart.application.command.DeleteCartItemCommand;
import com.book.core.cart.application.command.DeleteCartItemsCommand;
import com.book.core.cart.application.command.GetCartCommand;
import com.book.core.cart.application.result.GetCartResult;
import com.book.core.cart.application.usecase.AddCartItemUseCase;
import com.book.core.cart.application.usecase.DeleteCartItemUseCase;
import com.book.core.cart.application.usecase.DeleteCartItemsUseCase;
import com.book.core.cart.application.usecase.GetCartUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartService {
    private final AddCartItemUseCase addUseCase;
    private final DeleteCartItemUseCase deleteCartItemUseCase;
    private final DeleteCartItemsUseCase deleteCartItemsUseCase;
    private final GetCartUseCase getCartUseCase;

    public void addCartItem(final AddCartItemCommand command) {
        addUseCase.execute(command);
    }

    public GetCartResult getCart(final GetCartCommand command) {
        return getCartUseCase.execute(command);
    }

    public void deleteCartItem(final DeleteCartItemCommand command) {
        deleteCartItemUseCase.execute(command);
    }

    public void deleteCartItems(final DeleteCartItemsCommand command) {
        deleteCartItemsUseCase.execute(command);
    }
}
