package com.book.core.payment.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.port.OwnedCouponRepositoryPort;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.domain.Order;
import com.book.core.payment.application.command.CreatePaymentCommand;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.application.service.PaymentService;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.enums.PaymentState;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.domain.PointBalance;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class PaymentIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");
    @Autowired
    private PaymentService service;
    @Autowired
    private PaymentRepositoryPort payments;
    @Autowired
    private PaymentJpaRepository paymentJpaRepository;
    @Autowired
    private OrderRepositoryPort orders;
    @Autowired
    private PointBalanceRepositoryPort balances;
    @Autowired
    private OwnedCouponRepositoryPort coupons;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void 상품이_없는_주문은_결제를_저장하기_전에_거절한다() {
        final var order = orders.save(Order.create(1004L, "empty-order-" + UUID.randomUUID(), null));
        balances.save(new PointBalance(1004L));

        assertThatThrownBy(() -> service.create(new CreatePaymentCommand(1004L, order.key(), -1L, BigDecimal.ZERO))).isInstanceOfSatisfying(
            CoreException.class, (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.NOT_FOUND_DATA));
        assertThat(payments.findByOrderId(order.id())).isEmpty();
    }

    private Order order(final Long userId) {
        final long bookId = 700000L + userId;
        final long productId = 800000L + userId;
        jdbc.update("INSERT INTO books (id, title, author, publisher, category, published_at) VALUES (?, ?, ?, ?, ?, ?)", bookId,
            "결제 테스트 도서", "저자", "출판사", "소설", "2026-01-01");
        jdbc.update(
            "INSERT INTO products (id, book_id, name, sale_price, discounted_price, cost_price, stock_quantity) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)",
            productId, bookId, "결제 테스트 상품", BigDecimal.valueOf(10000), BigDecimal.valueOf(10000), BigDecimal.ZERO, 10);
        return new TransactionTemplate(transactionManager).execute((final var status) -> {
            final var order = Order.create(userId, "test-order-" + UUID.randomUUID(), null);
            order.addItem(productId, "책", "", "저자", BigDecimal.valueOf(10000), BigDecimal.valueOf(10000), 1);
            return orders.save(order);
        });
    }

    private static Payment payment(final Order order, final Long ownedCouponId) {
        return new Payment(order.userId(), order.id(), BigDecimal.valueOf(10000), ownedCouponId, BigDecimal.ZERO, BigDecimal.valueOf(1200),
            order.userId(), BigDecimal.valueOf(8800), PaymentState.READY);
    }
}
