package com.book.core.cart.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.cart.application.command.GetCartCommand;
import com.book.core.cart.application.port.CartItemRepositoryPort;
import com.book.core.cart.application.port.CartRepositoryPort;
import com.book.core.cart.application.result.GetCartItemResult;
import com.book.core.cart.application.result.GetCartResult;
import java.util.List;
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
                .findByUserId(command.userId())
                .map(cart -> new GetCartResult(cartItemRepository.findActiveByCartId(cart.id()).stream()
                        .map(item -> new GetCartItemResult(item.id(), item.productId(), item.quantity()))
                        .toList()))
                .orElseGet(() -> new GetCartResult(List.of()));
    }
}
