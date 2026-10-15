package com.book.core.cart.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.cart.application.port.CartRepositoryPort;
import com.book.core.cart.domain.Cart;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class RecoverCartUseCase {
    private final CartRepositoryPort cartRepository;

    @Transactional
    public boolean execute(final Long userId) {
        if (cartRepository.existsByUserId(userId)) {
            return false;
        }
        cartRepository.save(Cart.of(userId));
        return true;
    }
}
