package com.book.core.cart.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

class CartTest {
    @Test
    void 상품이_29개면_추가할_수_있다() {
        final var cart = new Cart(1L, 1L);

        assertThatCode(() -> cart.validateCanAddItem(29)).doesNotThrowAnyException();
    }

    @Test
    void 상품이_30개로_가득_차면_새_상품을_거부한다() {
        final var cart = new Cart(1L, 1L);

        assertThatThrownBy(() -> cart.validateCanAddItem(30))
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

    @Test
    void 활성_장바구니_상품을_삭제하면_DELETED_상태로_전이한다() {
        final var item = new CartItem(11L, 1L, 20L, 2);

        item.delete();

        assertThat(item.isDeleted()).isTrue();
        assertThat(item.isActive()).isFalse();
    }
}
