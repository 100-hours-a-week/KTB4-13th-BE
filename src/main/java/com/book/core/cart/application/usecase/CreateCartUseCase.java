package com.book.core.cart.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.cart.application.port.CartRepositoryPort;
import com.book.core.cart.domain.Cart;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateCartUseCase {
    private final CartRepositoryPort cartRepository;

    @Transactional
    public void execute(final Long userId) {
        cartRepository.save(Cart.of(userId));
    }
}
