package com.book.core.cart.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class CartTest {
    @Test
    void 상품을_추가한다() {
        final var cart = new Cart(1L, 1L, List.of());
        final var item = CartItem.from(1L, 20L, 2);

        final var added = cart.addItem(item);

        assertThat(added).isSameAs(item);
        assertThat(cart.items()).containsExactly(item);
    }

    @Test
    void 상품이_30개로_가득_차면_새_상품을_거부한다() {
        final var items = IntStream.rangeClosed(1, 30)
                .mapToObj((final var productId) -> CartItem.from(1L, (long) productId, 1))
                .toList();
        final var cart = new Cart(1L, 1L, items);

        assertThatThrownBy(() -> cart.addItem(CartItem.from(1L, 31L, 1)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_ITEM_LIMIT_EXCEEDED));
    }

    @Test
    void 잘못된_수량은_상품을_생성하지_않는다() {
        assertThatThrownBy(() -> CartItem.from(1L, 20L, 501))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_CART_ITEM_QUANTITY));
    }
}
