package com.book.core.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OrderTest {
    @Test
    void 배송지가_없어도_주문_객체를_생성한다() {
        final Order order = Order.create(42L, "order_without_address", null);

        assertThat(order.address()).isNull();
        assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void 주문상품_금액을_합산하고_주문과_품목을_CREATED로_생성한다() {
        final Order order = Order.create(42L, "order_test", OrderAddress.from("06236", "서울 주소", null));

        addOrderItem(order, 101L, 2, new BigDecimal("12.35"));
        addOrderItem(order, 102L, 3, new BigDecimal("0.01"));

        assertThat(order.totalPrice()).isEqualByComparingTo("24.73");
        assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.items()).extracting(OrderItem::unitPrice, OrderItem::totalPrice, OrderItem::quantity, OrderItem::status)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(new BigDecimal("12.35"), new BigDecimal("24.70"), 2, OrderItemStatus.CREATED),
                org.assertj.core.groups.Tuple.tuple(new BigDecimal("0.01"), new BigDecimal("0.03"), 3, OrderItemStatus.CREATED));
    }

    @Test
    void 주문상품_수량이_범위를_벗어나면_주문에_추가하지_않는다() {
        final Order order = Order.create(42L, "order_test", OrderAddress.from("06236", "서울 주소", null));

        assertThatThrownBy(() -> addOrderItem(order, 101L, 0, new BigDecimal("10.00"))).isInstanceOf(CoreException.class)
            .extracting(exception -> ((CoreException) exception).errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
        assertThatThrownBy(() -> addOrderItem(order, 101L, 501, new BigDecimal("10.00"))).isInstanceOf(CoreException.class);
        assertThat(order.items()).isEmpty();
        assertThat(order.totalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void 음수_단가로_주문상품을_생성하지_않는다() {
        final Order order = Order.create(42L, "order_test", OrderAddress.from("06236", "서울 주소", null));

        assertThatThrownBy(() -> addOrderItem(order, 101L, 1, new BigDecimal("-0.01"))).isInstanceOf(CoreException.class);
        assertThat(order.totalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private static void addOrderItem(final Order order, final Long productId, final Integer quantity, final BigDecimal unitPrice) {
        order.addItem(productId, "상품 " + productId, null, "저자", unitPrice, unitPrice, quantity);
    }
}
