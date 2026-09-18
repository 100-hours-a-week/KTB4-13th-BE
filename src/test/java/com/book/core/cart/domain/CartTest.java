package com.book.core.cart.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;

class CartTest {
    @Test
    void 동일_상품은_요청한_수량으로_대체하고_잘못된_수량이면_기존_수량을_유지한다() {
        final var cart = new Cart(1L, 1L, List.of(new CartItem(10L, 20L, 499)));
        assertThat(cart.add(20L, 1).quantity()).isEqualTo(1);
        assertThatThrownBy(() -> cart.add(20L, 501)).isInstanceOf(BusinessException.class);
        assertThat(cart.items().getFirst().quantity()).isEqualTo(1);
    }

    @Test
    void 수량변경이_1보다_작으면_1로_보정한다() {
        final var cart = new Cart(1L, 1L, List.of(new CartItem(Long.valueOf(10L), 20L, 2)));

        assertThat(cart.change(10L, 0).quantity()).isEqualTo(1);
    }
}
