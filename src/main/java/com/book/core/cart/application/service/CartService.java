package com.book.core.cart.application.service;

import com.book.core.cart.application.command.AddCartItemCommand;
import com.book.core.cart.application.command.GetCartCommand;
import com.book.core.cart.application.command.ModifyCartItemCommand;
import com.book.core.cart.application.result.GetCartResult;
import com.book.core.cart.application.usecase.AddCartItemUseCase;
import com.book.core.cart.application.usecase.GetCartUseCase;
import com.book.core.cart.application.usecase.ModifyCartItemUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartService {
    private final AddCartItemUseCase addUseCase;
    private final GetCartUseCase getCartUseCase;
    private final ModifyCartItemUseCase modifyCartItemUseCase;

    public void addCartItem(final AddCartItemCommand command) {
        addUseCase.execute(command);
    }

    public GetCartResult getCart(final GetCartCommand command) {
        return getCartUseCase.execute(command);
    }

    public void modifyCartItem(final ModifyCartItemCommand command) {
        modifyCartItemUseCase.execute(command);
    }
}
