package com.book.core.cart.application.usecase;

import com.book.core.cart.domain.Cart;
import com.book.core.cart.domain.CartItem;

import com.book.common.annotation.UseCase;
import com.book.core.cart.application.port.CartItemRepositoryPort;
import com.book.core.cart.application.port.CartRepositoryPort;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class DeleteCartItemsByProductsUseCase {
    private final CartRepositoryPort cartRepository;
    private final CartItemRepositoryPort cartItemRepository;

    @Transactional
    public void execute(final Long userId, final Collection<Long> productIds) {
        cartRepository.findByUserId(userId).ifPresent((final Cart cart) -> {
            cartItemRepository.findActiveByCartId(cart.id()).stream().filter((final CartItem item) -> productIds.contains(item.productId()))
                .forEach((final CartItem item) -> item.delete());
        });
    }
}
