package com.book.core.cart.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorType;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class CartTest {
    @Test
    void 같은_상품을_추가하면_요청한_수량으로_대체한다() {
        final var cart = new Cart(1L, 1L, List.of(new CartItem(Long.valueOf(10L), 20L, 2)));

        final var added = cart.add(20L, 4);

        assertThat(added.id()).isEqualTo(10L);
        assertThat(added.quantity()).isEqualTo(4);
        assertThat(cart.items()).extracting(CartItem::quantity).containsExactly(4);
    }

    @Test
    void 새로운_상품을_추가한다() {
        final var cart = new Cart(1L, 1L, List.of());

        final var added = cart.add(20L, 2);

        assertThat(added.id()).isNull();
        assertThat(added.productId()).isEqualTo(20L);
        assertThat(added.quantity()).isEqualTo(2);
    }

    @Test
    void 상품이_30개로_가득_차면_새_상품을_거부한다() {
        final var items = IntStream.rangeClosed(1, 30)
                .mapToObj((final var productId) -> new CartItem(Long.valueOf(productId), productId, 1))
                .toList();
        final var cart = new Cart(1L, 1L, items);

        assertThatThrownBy(() -> cart.add(31L, 1))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        (final var exception) ->
                                assertThat(exception.errorType()).isEqualTo(ErrorType.CART_ITEM_LIMIT_EXCEEDED));
    }

    @Test
    void 기존_상품의_잘못된_수량은_기존_수량을_변경하지_않는다() {
        final var cart = new Cart(1L, 1L, List.of(new CartItem(Long.valueOf(10L), 20L, 2)));

        assertThatThrownBy(() -> cart.add(20L, 501)).isInstanceOf(CoreException.class);

        assertThat(cart.items().getFirst().quantity()).isEqualTo(2);
    }
}
