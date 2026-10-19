package com.book.core.payment.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.domain.Order;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.enums.PaymentState;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class PaymentOrderKeyLookupIntegrationTest {
    @Autowired
    private OrderRepositoryPort orders;
    @Autowired
    private PaymentRepositoryPort payments;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void 주문키와_연결된_결제만_조회한다() {
        final Order first = order(910001L);
        final Order second = order(910002L);
        final Payment firstPayment = payments.save(payment(first));
        final Payment secondPayment = payments.save(payment(second));
        assertThat(payments.findByOrderKey(second.key())).get().extracting(Payment::id).isEqualTo(secondPayment.id());
        assertThat(payments.findByOrderKey(first.key())).get().extracting(Payment::id).isEqualTo(firstPayment.id());
        assertThat(payments.findByOrderKey("missing-" + UUID.randomUUID())).isEmpty();
    }

    @Test
    void 삭제된_주문의_결제는_주문키로_조회하지_않는다() {
        final Order order = order(910003L);
        payments.save(payment(order));
        jdbc.update("UPDATE orders SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?", order.id());
        assertThat(payments.findByOrderKey(order.key())).isEmpty();
    }

    private Order order(final Long userId) {
        final long bookId = 700000L + userId;
        final long productId = 800000L + userId;
        jdbc.update("INSERT INTO books (id, title, author, publisher, category, published_at, created_at, updated_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", bookId, "결제 조회 도서", "저자", "출판사", "소설", "2026-01-01");
        jdbc.update(
            "INSERT INTO products (id, book_id, name, sale_price, discounted_price, cost_price, stock_quantity, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            productId, bookId, "결제 조회 상품", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, 10);
        final Order order = Order.create(userId, "payment-lookup-" + UUID.randomUUID(), null);
        order.addItem(productId, "책", "", "저자", BigDecimal.TEN, BigDecimal.TEN, 1);
        return orders.save(order);
    }

    private Payment payment(final Order order) {
        return new Payment(order.userId(), order.id(), BigDecimal.TEN, -1L, BigDecimal.ZERO, BigDecimal.ZERO, 1L, BigDecimal.TEN,
            PaymentState.READY);
    }
}
