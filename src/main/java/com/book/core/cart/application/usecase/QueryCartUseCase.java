package com.book.core.cart.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.cart.application.command.CartQueryCommand;
import com.book.core.cart.application.port.CartItemRepositoryPort;
import com.book.core.cart.application.port.CartRepositoryPort;
import com.book.core.cart.application.result.CartItemQueryResult;
import com.book.core.cart.application.result.CartQueryResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class QueryCartUseCase {
    private final CartRepositoryPort cartRepository;
    private final CartItemRepositoryPort cartItemRepository;

    @Transactional(readOnly = true)
    public CartQueryResult execute(final CartQueryCommand command) {
        return cartRepository
                .findByUserId(command.userId())
                .map(cart -> new CartQueryResult(cartItemRepository.findActiveByCartId(cart.id()).stream()
                        .map(item -> new CartItemQueryResult(item.id(), item.productId(), item.quantity()))
                        .toList()))
                .orElseGet(() -> new CartQueryResult(List.of()));
    }
}
