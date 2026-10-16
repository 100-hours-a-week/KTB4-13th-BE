package com.book.core.payment.infrastructure.persistence.repository;

import org.springframework.transaction.TransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.port.OwnedCouponRepositoryPort;
import com.book.core.coupon.domain.OwnedCoupon;
import com.book.core.coupon.domain.enums.OwnedCouponStatus;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderStatus;
import com.book.core.payment.application.command.ApprovePaymentCommand;
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
    void 성공_트랜잭션은_주문_포인트_취소잔액_거래이력을_함께_저장한다() {
        final Order order = order(1001L);
        balances.save(new PointBalance(null, 1001L, BigDecimal.valueOf(5000)));
        final Payment payment = payments.save(payment(order, -1L));
        service.success(new ApprovePaymentCommand(order.key(), "test-pg-key", new BigDecimal("8800.00")));
        assertThat(payments.findById(payment.id()).orElseThrow().state()).isEqualTo(PaymentState.SUCCESS);
        assertThat(orders.findActiveOrderByKey(order.key()).orElseThrow().status()).isEqualTo(OrderStatus.PAID);
        assertThat(balances.findByUserId(1001L).orElseThrow().balance()).isEqualByComparingTo("5800");
        assertThat(jdbc.queryForObject("select count(*) from point_history where reference_id = ?", Long.class, payment.id()))
            .isEqualTo(2L);
        assertThat(
            jdbc.queryForObject("select cancellable_paid_amount from cancel_balance where order_id = ?", BigDecimal.class, order.id()))
            .isEqualByComparingTo("8800");
        assertThat(jdbc.queryForObject("select count(*) from transaction_history where payment_id = ? and type = 'PAYMENT'", Long.class,
            payment.id())).isEqualTo(1L);
    }

    @Test
    void 사용완료_쿠폰_실패는_앞서_변경한_결제와_주문도_롤백한다() {
        final Order order = order(1002L);
        final OwnedCoupon coupon = coupons.save(new OwnedCoupon(1002L, 123L, OwnedCouponStatus.USED, 1L, 1L));
        final Payment payment = payments.save(payment(order, coupon.id()));
        assertThatThrownBy(() -> service.success(new ApprovePaymentCommand(order.key(), "test-pg-key", new BigDecimal("8800.00"))))
            .isInstanceOf(CoreException.class);
        assertThat(payments.findById(payment.id()).orElseThrow().state()).isEqualTo(PaymentState.READY);
        assertThat(orders.findActiveOrderByKey(order.key()).orElseThrow().status()).isEqualTo(OrderStatus.CREATED);
        assertThat(jdbc.queryForObject("select count(*) from transaction_history where payment_id = ?", Long.class, payment.id())).isZero();
        assertThat(jdbc.queryForObject("select count(*) from cancel_balance where order_id = ?", Long.class, order.id())).isZero();
    }

    @Test
    void 상품이_없는_주문은_결제를_저장하기_전에_거절한다() {
        final Order order = orders.save(Order.create(1004L, "empty-order-" + UUID.randomUUID(), null));
        balances.save(new PointBalance(1004L));

        assertThatThrownBy(() -> service.create(new CreatePaymentCommand(1004L, order.key(), -1L, BigDecimal.ZERO))).isInstanceOfSatisfying(
            CoreException.class, (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.NOT_FOUND_DATA));
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
        return new TransactionTemplate(transactionManager).execute((final TransactionStatus status) -> {
            final Order order = Order.create(userId, "test-order-" + UUID.randomUUID(), null);
            order.addItem(productId, "책", "", "저자", BigDecimal.valueOf(10000), BigDecimal.valueOf(10000), 1);
            return orders.save(order);
        });
    }

    private static Payment payment(final Order order, final Long ownedCouponId) {
        return new Payment(order.userId(), order.id(), BigDecimal.valueOf(10000), ownedCouponId, BigDecimal.ZERO, BigDecimal.valueOf(1200),
            order.userId(), BigDecimal.valueOf(8800), PaymentState.READY);
    }
}
