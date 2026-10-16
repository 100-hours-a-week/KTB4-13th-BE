package com.book.core.cancel.infrastructure.persistence.repository;

import org.springframework.transaction.TransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.core.cancel.application.command.CancelCommand;
import com.book.core.cancel.application.port.CancelBalanceRepositoryPort;
import com.book.core.cancel.application.service.CancelService;
import com.book.core.cancel.domain.CancelAction;
import com.book.core.cancel.domain.CancelBalance;
import com.book.core.coupon.application.port.OwnedCouponRepositoryPort;
import com.book.core.coupon.domain.OwnedCoupon;
import com.book.core.coupon.domain.enums.OwnedCouponStatus;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderStatus;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.enums.PaymentMethod;
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
import org.springframework.dao.DataIntegrityViolationException;
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
class CancelIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");
    @Autowired
    private CancelService service;
    @Autowired
    private OrderRepositoryPort orders;
    @Autowired
    private PaymentRepositoryPort payments;
    @Autowired
    private CancelBalanceRepositoryPort balances;
    @Autowired
    private PointBalanceRepositoryPort points;
    @Autowired
    private OwnedCouponRepositoryPort coupons;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void 전체취소는_쿠폰_포인트_잔액_주문_취소내역과_거래이력을_같이_커밋한다() {
        final Order order = fixture(10001L, 5, true, true);
        service.cancel(new CancelCommand(10001L, new CancelAction(order.key())));
        assertThat(orders.findActiveOrderByKey(order.key()).orElseThrow().status()).isEqualTo(OrderStatus.CANCELED);
        assertThat(balances.findByOrderId(order.id()).orElseThrow().totalCanceledAmount()).isEqualByComparingTo("50000");
        assertThat(coupons.findByUserIdAndCouponId(10001L, 10001L).orElseThrow().status()).isEqualTo(OwnedCouponStatus.ISSUED);
        assertThat(points.findByUserId(10001L).orElseThrow().balance()).isEqualByComparingTo("18000");
        assertThat(jdbc.queryForObject("select count(*) from `cancel` where order_id = ? and type = 'ALL'", Long.class, order.id()))
            .isEqualTo(1L);
        assertThat(
            jdbc.queryForObject("select count(*) from transaction_history where order_id = ? and type = 'CANCEL'", Long.class, order.id()))
            .isEqualTo(1L);

        assertThat(jdbc.queryForObject("select canceled_quantity from order_item where order_id = ?", Long.class, order.id())).isZero();
    }

    @Test
    void 쿠폰_복원_실패는_앞서_변경한_주문도_롤백하고_취소내역을_남기지_않는다() {
        final Order order = fixture(10004L, 5, true, false);
        assertThatThrownBy(() -> service.cancel(new CancelCommand(10004L, new CancelAction(order.key()))))
            .isInstanceOf(CoreException.class);
        assertThat(orders.findActiveOrderByKey(order.key()).orElseThrow().status()).isEqualTo(OrderStatus.PAID);
        assertThat(balances.findByOrderId(order.id()).orElseThrow().totalCanceledAmount()).isEqualByComparingTo("0");
        assertThat(jdbc.queryForObject("select count(*) from `cancel` where order_id = ?", Long.class, order.id())).isZero();
    }

    @Test
    void 거래이력_저장실패는_쿠폰과_포인트_잔액과_취소내역까지_롤백한다() {
        final Order order = fixture(10005L, 5, true, true);
        jdbc.update("update payment set external_payment_key = null where order_id = ?", order.id());
        assertThatThrownBy(() -> service.cancel(new CancelCommand(10005L, new CancelAction(order.key()))))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(orders.findActiveOrderByKey(order.key()).orElseThrow().status()).isEqualTo(OrderStatus.PAID);
        assertThat(coupons.findByUserIdAndCouponId(10005L, 10005L).orElseThrow().status()).isEqualTo(OwnedCouponStatus.USED);
        assertThat(points.findByUserId(10005L).orElseThrow().balance()).isEqualByComparingTo("20000");
        assertThat(balances.findByOrderId(order.id()).orElseThrow().totalCanceledAmount()).isEqualByComparingTo("0");
        assertThat(jdbc.queryForObject("select count(*) from `cancel` where order_id = ?", Long.class, order.id())).isZero();
        assertThat(jdbc.queryForObject("select count(*) from point_history where user_id = ?", Long.class, 10005L)).isZero();
    }

    private Long itemId(final Order order) {
        return jdbc.queryForObject("select id from order_item where order_id = ?", Long.class, order.id());
    }

    private Order fixture(final Long userId, final int quantity, final boolean hasCoupon, final boolean couponUsed) {
        final long bookId = 900000L + userId;
        final long productId = 1000000L + userId;
        jdbc.update("INSERT INTO books (id, title, author, publisher, category, published_at) VALUES (?, ?, ?, ?, ?, ?)", bookId,
            "취소 테스트 도서", "저자", "출판사", "소설", "2026-01-01");
        jdbc.update(
            "INSERT INTO products (id, book_id, name, sale_price, discounted_price, cost_price, stock_quantity) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)",
            productId, bookId, "취소 테스트 상품", BigDecimal.valueOf(10000), BigDecimal.valueOf(10000), BigDecimal.ZERO, 10);
        return new TransactionTemplate(transactionManager).execute((final TransactionStatus status) -> {
            final Order order = Order.create(userId, "test-cancel-" + UUID.randomUUID(), null);
            order.addItem(productId, "책", "", "저자", BigDecimal.valueOf(10000), BigDecimal.valueOf(10000), quantity);
            order.paid();
            orders.save(order);
            Long ownedCouponId = -1L;
            BigDecimal couponDiscount = BigDecimal.ZERO;
            BigDecimal usedPoint = BigDecimal.valueOf(15000);
            if (hasCoupon) {
                jdbc.update(
                    "INSERT INTO coupon (id, name, type, discount, max_use_count, min_order_amount, expired_at, created_at, updated_at) "
                        + "VALUES (?, '취소 테스트', 'FIXED_AMOUNT', 10000, 1, 35000, DATE_ADD(NOW(), INTERVAL 7 DAY), NOW(), NOW())",
                    userId);
                OwnedCouponStatus couponStatus = OwnedCouponStatus.ISSUED;
                long usedCount = 0L;
                if (couponUsed) {
                    couponStatus = OwnedCouponStatus.USED;
                    usedCount = 1L;
                }
                ownedCouponId = coupons.save(new OwnedCoupon(userId, userId, couponStatus, 1L, usedCount)).id();
                couponDiscount = BigDecimal.valueOf(10000);
                usedPoint = BigDecimal.ZERO;
            }
            points.save(new PointBalance(null, userId, BigDecimal.valueOf(20000)));
            final BigDecimal paidAmount = order.totalPrice().subtract(couponDiscount).subtract(usedPoint);
            final Payment payment = new Payment(userId, order.id(), order.totalPrice(), ownedCouponId, couponDiscount, usedPoint, userId,
                paidAmount, PaymentState.READY);
            payment.success("test-pg", PaymentMethod.CARD, "test");
            payments.save(payment);
            balances.save(
                new CancelBalance(order.id(), paidAmount, usedPoint, couponDiscount, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
            return order;
        });
    }
}
