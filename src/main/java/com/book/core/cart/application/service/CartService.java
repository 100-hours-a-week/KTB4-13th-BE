package com.book.core.cart.application.service;

import com.book.core.cart.application.command.AddCartItemCommand;
import com.book.core.cart.application.command.CartQueryCommand;
import com.book.core.cart.application.result.CartQueryResult;
import com.book.core.cart.application.usecase.AddCartItemUseCase;
import com.book.core.cart.application.usecase.QueryCartUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartService {
    private final AddCartItemUseCase addUseCase;
    private final QueryCartUseCase queryUseCase;

    public void addCartItem(final AddCartItemCommand command) {
        addUseCase.execute(command);
    }

    public CartQueryResult getCart(final CartQueryCommand command) {
        return queryUseCase.execute(command);
    }
}
