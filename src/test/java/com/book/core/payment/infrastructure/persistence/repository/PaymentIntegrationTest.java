package com.book.core.payment.infrastructure.persistence.repository;

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
import com.book.core.payment.application.command.FailPaymentCommand;
import com.book.core.payment.application.port.PaymentAttemptRepositoryPort;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.application.service.PaymentService;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.PaymentAttempt;
import com.book.core.payment.domain.enums.PaymentState;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.application.usecase.ReservePaymentPointsUseCase;
import com.book.core.point.domain.PointBalance;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class PaymentIntegrationTest {
    @Autowired
    private PaymentService service;
    @Autowired
    private PaymentRepositoryPort payments;
    @Autowired
    private PaymentAttemptRepositoryPort attempts;
    @Autowired
    private ReservePaymentPointsUseCase reserve;
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

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.book.core.payment.application.port.PaymentClient pg;

    private void stubApproval(final PaymentAttempt attempt) {
        org.mockito.Mockito.when(pg.find("test-pg-key"))
            .thenReturn(new com.book.core.payment.application.result.PgPaymentResult("test-pg-key", attempt.pgOrderId(),
                new BigDecimal("8800"), new BigDecimal("8800"), "DONE", com.book.core.payment.domain.enums.PaymentMethod.CARD, "12345678",
                java.util.List.of()));
    }

    @Test
    void 성공_트랜잭션은_주문_포인트_취소잔액_거래이력을_함께_저장한다() {
        final Order order = order(1001L);
        final Fixture fixture = fixture(order, -1L);
        final Payment payment = fixture.payment();
        stubApproval(fixture.attempt());
        service.success(new ApprovePaymentCommand(fixture.attempt().pgOrderId(), "test-pg-key", new BigDecimal("8800.00")));
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
        final Fixture fixture = fixture(order, coupon.id());
        final Payment payment = fixture.payment();
        stubApproval(fixture.attempt());
        assertThatThrownBy(
            () -> service.success(new ApprovePaymentCommand(fixture.attempt().pgOrderId(), "test-pg-key", new BigDecimal("8800.00"))))
            .isInstanceOfSatisfying(CoreException.class,
                (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.OWNED_COUPON_INVALID_USAGE));
        assertThat(payments.findById(payment.id()).orElseThrow().state()).isEqualTo(PaymentState.READY);
        assertThat(orders.findActiveOrderByKey(order.key()).orElseThrow().status()).isEqualTo(OrderStatus.CREATED);
        assertThat(jdbc.queryForObject("select count(*) from transaction_history where payment_id = ?", Long.class, payment.id())).isZero();
        assertThat(jdbc.queryForObject("select count(*) from cancel_balance where order_id = ?", Long.class, order.id())).isZero();
        assertThat(balances.findByUserId(1002L).orElseThrow().reserved()).isEqualByComparingTo("1200");
    }

    @Test
    void 실패_콜백은_확인_대상을_커밋하고_중복_결제의_고유_제약은_기술예외를_전파한다() {
        final Order order = order(1003L);
        final Fixture fixture = fixture(order, -1L);
        final Payment payment = fixture.payment();
        assertThatThrownBy(() -> service.fail(1003L, new FailPaymentCommand(fixture.attempt().pgOrderId(), "FAIL", "test failure")))
            .isInstanceOfSatisfying(CoreException.class,
                (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.PG_PENDING));
        assertThat(payments.findById(payment.id()).orElseThrow().state()).isEqualTo(PaymentState.READY);
        assertThat(jdbc.queryForObject("select count(*) from pg_operation where order_key = ? and review_required = true", Long.class,
            order.key())).isEqualTo(1L);
        assertThat(jdbc.queryForObject("select paid_amount from transaction_history where payment_id = ?", BigDecimal.class, payment.id()))
            .isEqualByComparingTo("-1");
        assertThatThrownBy(() -> paymentJpaRepository.saveAndFlush(payment(order, -1L)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 상품이_없는_주문은_결제를_저장하기_전에_거절한다() {
        final Order unsaved = Order.create(1004L, "empty-order-" + UUID.randomUUID(), null);
        unsaved.reserveUntil(LocalDateTime.now().plusMinutes(10));
        final Order order = orders.save(unsaved);
        balances.save(new PointBalance(1004L));

        assertThatThrownBy(() -> service.create(new CreatePaymentCommand(1004L, order.key(), -1L, BigDecimal.ZERO, "empty-order-request")))
            .isInstanceOfSatisfying(CoreException.class,
                (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.NOT_FOUND_DATA));
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
            order.reserveUntil(LocalDateTime.now().plusMinutes(10));
            order.addItem(productId, "책", "", "저자", BigDecimal.valueOf(10000), BigDecimal.valueOf(10000), 1);
            return orders.save(order);
        });
    }

    private static Payment payment(final Order order, final Long ownedCouponId) {
        return new Payment(order.userId(), order.id(), BigDecimal.valueOf(10000), ownedCouponId, BigDecimal.ZERO, BigDecimal.valueOf(1200),
            order.userId(), BigDecimal.valueOf(8800), PaymentState.READY);
    }

    private Fixture fixture(final Order order, final Long couponId) {
        balances.save(new PointBalance(null, order.userId(), BigDecimal.valueOf(5000)));
        final Payment payment = payments.save(payment(order, couponId));
        final PaymentAttempt attempt = attempts
            .save(new PaymentAttempt(payment.id(), order.key(), "payment_" + UUID.randomUUID(), "request_" + UUID.randomUUID(), "hash"));
        reserve.execute(order.userId(), attempt.id(), payment.usedPoint(), payment.id());
        return new Fixture(payment, attempt);
    }

    private record Fixture(Payment payment, PaymentAttempt attempt) {
    }
}
