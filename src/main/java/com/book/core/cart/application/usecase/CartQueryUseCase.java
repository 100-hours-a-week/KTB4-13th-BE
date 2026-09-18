package com.book.core.cart.application.usecase;

import com.book.core.cart.application.port.CartRepository;
import com.book.core.cart.application.result.CartItemResult;
import com.book.core.cart.application.result.CartQueryResult;
import com.book.core.cart.domain.CartItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartQueryUseCase {
    private final CartRepository cartRepository;

    @Transactional(readOnly = true)
    public CartQueryResult execute(final long userId) {
        CartItem.requireId(userId);
        final var cart = cartRepository.findByUserId(userId);
        if (cart.isEmpty()) {
            return new CartQueryResult(List.of());
        }
        return new CartQueryResult(cart.get().items().stream()
                .map((final var item) -> new CartItemResult(item.id(), item.productId(), item.quantity()))
                .toList());
    }
}
