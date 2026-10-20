package com.book.core.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
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
    void 예약_만료_시각이_지난_미결제_주문만_만료하고_상품별_예약_수량을_합산한다() {
        final LocalDateTime expiresAt = LocalDateTime.of(2026, 10, 20, 10, 10);
        final Order order = Order.create(42L, "order_expire", null);
        addOrderItem(order, 101L, 2, BigDecimal.TEN);
        addOrderItem(order, 101L, 1, BigDecimal.TEN);
        addOrderItem(order, 102L, 4, BigDecimal.TEN);
        order.reserveUntil(expiresAt);

        assertThat(order.reservedQuantities()).containsExactlyInAnyOrderEntriesOf(Map.of(101L, 3, 102L, 4));
        assertThat(order.expire(expiresAt.minusNanos(1000))).isFalse();
        assertThat(order.expire(expiresAt)).isTrue();
        assertThat(order.status()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(order.expire(expiresAt.plusMinutes(1))).isFalse();
    }

    @Test
    void 예약_없이_만든_기존_주문은_만료하거나_반환할_예약이_없다() {
        final Order order = Order.create(42L, "order_legacy", null);
        addOrderItem(order, 101L, 2, BigDecimal.TEN);

        assertThat(order.hasReservation()).isFalse();
        assertThat(order.reservedQuantities()).isEmpty();
        assertThat(order.expire(LocalDateTime.MAX)).isFalse();
        assertThatThrownBy(() -> order.reserveUntil(null)).isInstanceOf(CoreException.class);
    }

    @Test
    void 대표_상품은_취소_여부와_무관하게_첫_주문상품이다() {
        final Order order = Order.create(42L, "order_representative", null);
        addOrderItem(order, 101L, 1, BigDecimal.TEN);
        addOrderItem(order, 102L, 2, BigDecimal.TEN);
        order.items().getFirst().cancel(1L);

        assertThat(order.representativeItem().itemName()).isEqualTo("상품 101");
    }

    @Test
    void 주문상품이_없는_주문의_대표_상품은_정합성_오류다() {
        final Order order = Order.create(42L, "order_without_items", null);

        assertThatThrownBy(order::representativeItem).isInstanceOf(CoreException.class)
            .extracting(exception -> ((CoreException) exception).errorCode()).isEqualTo(ErrorCode.ORDER_DATA_INCONSISTENT);
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

    @Test
    void 주문_전체_취소는_주문과_주문상품을_취소하고_취소시각을_기록한다() {
        final Order order = Order.create(42L, "order_test", OrderAddress.from("06236", "서울 주소", null));
        order.addItem(101L, "상품 101", null, "저자", new BigDecimal("10.00"), new BigDecimal("10.00"), 1);
        order.addItem(102L, "상품 102", null, "저자", new BigDecimal("5.00"), new BigDecimal("5.00"), 2);

        order.cancel();

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELED);
        assertThat(order.canceledAt()).isNotNull();
        assertThat(order.totalPrice()).isEqualByComparingTo("20.00");
        assertThat(order.items()).extracting(OrderItem::status).containsExactly(OrderItemStatus.CANCELED, OrderItemStatus.CANCELED);
    }

    @Test
    void 이미_취소된_주문은_다시_취소할_수_없다() {
        final Order order = Order.create(42L, "order_test", null);
        order.cancel();

        assertThatThrownBy(order::cancel).isInstanceOf(CoreException.class).extracting(exception -> ((CoreException) exception).errorCode())
            .isEqualTo(ErrorCode.ORDER_CANNOT_BE_CANCELED);
    }

    private static void addOrderItem(final Order order, final Long productId, final Integer quantity, final BigDecimal unitPrice) {
        order.addItem(productId, "상품 " + productId, null, "저자", unitPrice, unitPrice, quantity);
    }
}
