package com.book.core.cart.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.cart.application.command.GetCartCommand;
import com.book.core.cart.application.port.CartItemRepositoryPort;
import com.book.core.cart.application.port.CartRepositoryPort;
import com.book.core.cart.application.result.GetCartItemResult;
import com.book.core.cart.application.result.GetCartResult;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetCartUseCase {
    private final CartRepositoryPort cartRepository;
    private final CartItemRepositoryPort cartItemRepository;

    @Transactional(readOnly = true)
    public GetCartResult execute(final GetCartCommand command) {
        return cartRepository
                .findActiveByUserId(command.userId())
                .map(cart -> GetCartResult.of(cartItemRepository.findActiveByCartId(cart.id()).stream()
                        .map(GetCartItemResult::from)
                        .toList()))
                .orElseGet(GetCartResult::empty);
    }
}
