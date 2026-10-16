package com.book.core.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OrderItemPartialCancelTest {
    @Test
    void 누적_취소_수량에_따라_부분취소와_전체취소_상태를_정한다() {
        final Order order = order();
        final OrderItem item = order.items().getFirst();
        item.cancel(1L);
        assertThat(item.canceledQuantity()).isEqualTo(1);
        assertThat(item.cancellableQuantity()).isEqualTo(2);
        assertThat(item.status()).isEqualTo(OrderItemStatus.PARTIAL_CANCELED);
        item.cancel(2L);
        assertThat(item.isAllCanceled()).isTrue();
        assertThat(item.cancellableQuantity()).isZero();
        assertThat(item.status()).isEqualTo(OrderItemStatus.CANCELED);
        assertThatThrownBy(() -> item.cancel(1L)).isInstanceOf(CoreException.class);
    }

    @Test
    void 결제_후_전체취소는_참고처럼_주문상태만_바꾸고_상품의_취소수량은_유지한다() {
        final Order order = order();
        order.canceled();
        assertThat(order.status()).isEqualTo(OrderStatus.CANCELED);
        assertThat(order.items().getFirst().canceledQuantity()).isZero();
        assertThat(order.items().getFirst().status()).isEqualTo(OrderItemStatus.PAID);
    }

    private static Order order() {
        final Order order = Order.create(1L, "order", null);
        order.addItem(10L, "책", "", "저자", BigDecimal.TEN, BigDecimal.TEN, 3);
        order.paid();
        return order;
    }
}
