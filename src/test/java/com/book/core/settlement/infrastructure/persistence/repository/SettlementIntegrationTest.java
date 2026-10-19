package com.book.core.settlement.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.cancel.application.port.CancelRepositoryPort;
import com.book.core.cancel.domain.Cancel;
import com.book.core.cancel.domain.enums.CancelType;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.domain.Order;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.enums.PaymentMethod;
import com.book.core.payment.domain.enums.PaymentState;
import com.book.core.settlement.application.port.SettlementRepositoryPort;
import com.book.core.settlement.application.port.SettlementTargetRepositoryPort;
import com.book.core.settlement.application.service.SettlementService;
import com.book.core.settlement.domain.Settlement;
import com.book.core.settlement.domain.SettlementState;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles({"test", "mysql-test"})
@Testcontainers
@Tag("integration")
class SettlementIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");
    @Autowired
    private SettlementService service;
    @Autowired
    private SettlementRepositoryPort settlements;
    @Autowired
    private SettlementTargetRepositoryPort targets;
    @Autowired
    private PaymentRepositoryPort payments;
    @Autowired
    private CancelRepositoryPort cancels;
    @Autowired
    private OrderRepositoryPort orders;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private JdbcTemplate jdbc;
    private final LocalDate date = LocalDate.of(2026, 10, 16);

    @BeforeEach
    void clearData() {
        jdbc.update("DELETE FROM settlement_target");
        jdbc.update("DELETE FROM settlement");
        jdbc.update("DELETE FROM merchant_product_mapping");
        jdbc.update("DELETE FROM merchant");
    }

    @Test
    void 결제와_부분취소를_적재하고_가맹점별_생성_및_SENT_전환한다() {
        final var order = order(101L);
        final var paidAt = date.minusDays(1).atTime(12, 0);
        payments.save(payment(order, paidAt));
        cancels.save(cancel(order, paidAt.plusHours(1), CancelType.PARTIAL));
        merchant(71L, order.items().getFirst().productId());
        settlements.saveAll(List.of(settlement(71L, date.minusDays(1), "1000", SettlementState.SENT)));
        service.loadTargets(date, date.minusDays(1).atStartOfDay(), date.atStartOfDay().minusNanos(1));
        final var summary = targets.findSummary(date).getFirst();
        assertThat(summary.merchantId()).isEqualTo(71L);
        assertThat(summary.targetCount()).isEqualTo(2);
        assertThat(summary.orderCount()).isEqualTo(1);
        assertThat(summary.targetAmount()).isEqualByComparingTo("200");
        assertThat(service.generate(date)).isEqualTo(1);
        final var ready = settlements.findByState(SettlementState.READY).getFirst();
        assertThat(ready.settlementAmount()).isEqualByComparingTo("180");
        service.transfer(date);
        assertThat(settlements.findByState(SettlementState.READY)).isEmpty();
        assertThat(settlements.findAllById(List.of(ready.id())).getFirst().state()).isEqualTo(SettlementState.SENT);
    }

    @Test
    void 전체취소는_원주문금액을_상쇄하고_영원은_이체전환하지_않는다() {
        final var order = order(102L);
        final var paidAt = date.minusDays(1).atTime(12, 0);
        payments.save(payment(order, paidAt));
        cancels.save(cancel(order, paidAt.plusHours(1), CancelType.ALL));
        merchant(72L, order.items().getFirst().productId());
        settlements.saveAll(List.of(settlement(72L, date.minusDays(1), "1000", SettlementState.SENT)));
        service.loadTargets(date, date.minusDays(1).atStartOfDay(), date.atStartOfDay().minusNanos(1));
        assertThat(targets.findSummary(date).getFirst().targetAmount()).isEqualByComparingTo("0");
        assertThat(service.generate(date)).isEqualTo(1);
        service.transfer(date);
        assertThat(settlements.findByState(SettlementState.READY)).hasSize(1);
    }

    private Settlement settlement(final Long merchantId, final LocalDate day, final String originalAmount, final SettlementState state) {
        return new Settlement(merchantId, day, new BigDecimal(originalAmount), BigDecimal.ZERO, new BigDecimal("0.1"),
            new BigDecimal(originalAmount), state);
    }

    private void merchant(final Long merchantId, final Long productId) {
        jdbc.update("INSERT INTO merchant (id, name, settlement_cycle, created_at, updated_at) VALUES (?, '테스트 가맹점', 1, NOW(), NOW())",
            merchantId);
        jdbc.update("INSERT INTO merchant_product_mapping (merchant_id, product_id, created_at, updated_at) VALUES (?, ?, NOW(), NOW())",
            merchantId, productId);
    }

    private Order order(final Long userId) {
        final long bookId = 700000L + userId;
        final long productId = 800000L + userId;
        jdbc.update("INSERT INTO books (id, title, author, publisher, category, published_at) VALUES (?, ?, ?, ?, ?, ?)", bookId,
            "정산 테스트 도서", "저자", "출판사", "소설", "2026-01-01");
        jdbc.update(
            "INSERT INTO products (id, book_id, name, sale_price, discounted_price, cost_price, stock_quantity) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)",
            productId, bookId, "정산 상품", BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.ZERO, 10);
        return new TransactionTemplate(transactionManager).execute((final var status) -> {
            final var order = Order.create(userId, "settlement-" + UUID.randomUUID(), null);
            order.addItem(productId, "책", "", "저자", BigDecimal.valueOf(100), BigDecimal.valueOf(100), 3);
            return orders.save(order);
        });
    }

    private Payment payment(final Order order, final LocalDateTime paidAt) {
        final var payment = new Payment(order.userId(), order.id(), BigDecimal.valueOf(300), -1L, BigDecimal.ZERO, BigDecimal.ZERO,
            order.userId(), BigDecimal.valueOf(300), PaymentState.READY);
        payment.success("test-settlement", PaymentMethod.CARD, "test");
        ReflectionTestUtils.setField(payment, "paidAt", paidAt);
        return payment;
    }

    private Cancel cancel(final Order order, final LocalDateTime canceledAt, final CancelType type) {
        return new Cancel(order.userId(), type, order.id(), order.items().getFirst().id(), 1L, BigDecimal.valueOf(300), -1L,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(300), 1L, BigDecimal.valueOf(100), BigDecimal.ZERO, BigDecimal.ZERO,
            "test-cancel", canceledAt);
    }
}
