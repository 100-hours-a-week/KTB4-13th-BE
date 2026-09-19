package com.book.core.cart.application.service;

import com.book.core.cart.application.command.AddCartItemCommand;
import com.book.core.cart.application.usecase.AddCartItemUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartService {
    private final AddCartItemUseCase addUseCase;

    public void addCartItem(final AddCartItemCommand command) {
        addUseCase.execute(command);
    }
}
