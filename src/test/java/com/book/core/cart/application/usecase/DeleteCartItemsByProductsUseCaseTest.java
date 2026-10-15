package com.book.core.cart.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.core.cart.application.port.CartItemRepositoryPort;
import com.book.core.cart.application.port.CartRepositoryPort;
import com.book.core.cart.domain.Cart;
import com.book.core.cart.domain.CartItem;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DeleteCartItemsByProductsUseCaseTest {
    private final CartRepositoryPort carts = mock(CartRepositoryPort.class);
    private final CartItemRepositoryPort items = mock(CartItemRepositoryPort.class);

    @Test
    void 결제한_상품만_삭제하고_나머지는_유지한다() {
        final var purchased = new CartItem(1L, 10L, 100L, 1);
        final var other = new CartItem(2L, 10L, 200L, 1);
        when(carts.findByUserId(42L)).thenReturn(Optional.of(new Cart(10L, 42L)));
        when(items.findActiveByCartId(10L)).thenReturn(List.of(purchased, other));
        new DeleteCartItemsByProductsUseCase(carts, items).execute(42L, List.of(100L));
        assertThat(purchased.isDeleted()).isTrue();
        assertThat(other.isActive()).isTrue();
    }

    @Test
    void 장바구니가_없으면_상품_조회와_삭제를_생략한다() {
        when(carts.findByUserId(42L)).thenReturn(Optional.empty());
        new DeleteCartItemsByProductsUseCase(carts, items).execute(42L, List.of(100L));
        verifyNoInteractions(items);
    }
}
