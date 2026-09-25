package com.book.core.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OrderTest {
    @Test
    void 주문상품_금액을_합산하고_주문과_품목을_CREATED로_생성한다() {
        final Order order = Order.create(42L, "order_test", OrderAddress.from("06236", "서울 주소", null));

        order.addItem(101L, new BigDecimal("12.35"), 2);
        order.addItem(102L, new BigDecimal("0.01"), 3);

        assertThat(order.totalPrice()).isEqualByComparingTo("24.73");
        assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.items())
                .extracting(OrderItem::unitPrice, OrderItem::totalPrice, OrderItem::quantity, OrderItem::status)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                new BigDecimal("12.35"), new BigDecimal("24.70"), 2, OrderItemStatus.CREATED),
                        org.assertj.core.groups.Tuple.tuple(
                                new BigDecimal("0.01"), new BigDecimal("0.03"), 3, OrderItemStatus.CREATED));
    }

    @Test
    void 주문상품_수량이_범위를_벗어나면_주문에_추가하지_않는다() {
        final Order order = Order.create(42L, "order_test", OrderAddress.from("06236", "서울 주소", null));

        assertThatThrownBy(() -> order.addItem(101L, new BigDecimal("10.00"), 0))
                .isInstanceOf(CoreException.class)
                .extracting(exception -> ((CoreException) exception).errorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
        assertThatThrownBy(() -> order.addItem(101L, new BigDecimal("10.00"), 501))
                .isInstanceOf(CoreException.class);
        assertThat(order.items()).isEmpty();
        assertThat(order.totalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void 음수_단가로_주문상품을_생성하지_않는다() {
        final Order order = Order.create(42L, "order_test", OrderAddress.from("06236", "서울 주소", null));

        assertThatThrownBy(() -> order.addItem(101L, new BigDecimal("-0.01"), 1))
                .isInstanceOf(CoreException.class);
        assertThat(order.totalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
