package com.book.core.cart.application.usecase;

import com.book.core.cart.application.command.CartAddCommand;
import com.book.core.cart.application.port.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartAddUseCase {
    private final CartRepository cartRepository;

    @Transactional
    public void execute(final CartAddCommand command) {
        final var cart = cartRepository.findOrCreate(command.userId());
        final var item = cart.add(command.productId(), command.quantity());
        cartRepository.saveItem(cart.id(), item);
    }
}
