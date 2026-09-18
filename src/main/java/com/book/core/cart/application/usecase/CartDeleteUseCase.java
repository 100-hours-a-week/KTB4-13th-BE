package com.book.core.cart.application.usecase;

import com.book.core.cart.application.command.CartDeleteCommand;
import com.book.core.cart.application.port.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartDeleteUseCase {
    private final CartRepository cartRepository;

    @Transactional
    public void execute(final CartDeleteCommand command) {
        cartRepository
                .lockByUserId(command.userId())
                .ifPresent((final var cart) -> cartRepository.deleteItems(cart.id(), command.cartItemIds()));
    }
}
