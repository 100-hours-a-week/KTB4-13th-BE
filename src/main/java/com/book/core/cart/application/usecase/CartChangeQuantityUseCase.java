package com.book.core.cart.application.usecase;

import com.book.common.exception.BusinessException;
import com.book.core.cart.application.command.CartChangeQuantityCommand;
import com.book.core.cart.application.port.CartRepository;
import com.book.core.cart.domain.exception.CartErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartChangeQuantityUseCase {
    private final CartRepository cartRepository;

    @Transactional
    public void execute(final CartChangeQuantityCommand command) {
        final var cart = cartRepository
                .lockByUserId(command.userId())
                .orElseThrow(() -> new BusinessException(CartErrorCode.FORBIDDEN));
        cartRepository.saveItem(cart.id(), cart.change(command.cartItemId(), command.quantity()));
    }
}
