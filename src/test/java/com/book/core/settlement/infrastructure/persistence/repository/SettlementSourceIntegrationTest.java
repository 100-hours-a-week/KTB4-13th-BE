package com.book.core.settlement.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.cancel.application.port.CancelRepositoryPort;
import com.book.core.cancel.application.usecase.GetCancelsForSettlementUseCase;
import com.book.core.cancel.domain.Cancel;
import com.book.core.cancel.domain.enums.CancelType;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.application.usecase.GetOrderItemsForSettlementUseCase;
import com.book.core.order.domain.Order;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.application.usecase.GetPaymentsForSettlementUseCase;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.enums.PaymentMethod;
import com.book.core.payment.domain.enums.PaymentState;
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
class SettlementSourceIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");
    @Autowired
    private GetPaymentsForSettlementUseCase paymentReader;
    @Autowired
    private GetCancelsForSettlementUseCase cancelReader;
    @Autowired
    private GetOrderItemsForSettlementUseCase orderReader;
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
    void 결제취소조회는_기간경계를_포함하고_ID_오름차순으로_페이지를_나눈다() {
        final var from = date.minusDays(10).atStartOfDay();
        final var to = from.plusHours(1);
        final var firstOrder = order(103L);
        final var secondOrder = order(104L);
        final var thirdOrder = order(105L);
        final var first = payments.save(payment(firstOrder, from));
        final var second = payments.save(payment(secondOrder, to));
        payments.save(payment(thirdOrder, to.plusNanos(1000)));
        final var firstPage = paymentReader.execute(from, to, 0, 1);
        assertThat(firstPage.content()).extracting(Payment::id).containsExactly(first.id());
        assertThat(firstPage.hasNext()).isTrue();
        final var lastPage = paymentReader.execute(from, to, 1, 1);
        assertThat(lastPage.content()).extracting(Payment::id).containsExactly(second.id());
        assertThat(lastPage.hasNext()).isFalse();
        jdbc.update("UPDATE payment SET state = 'READY' WHERE id = ?", first.id());
        assertThat(paymentReader.execute(from, to, 0, 10).content()).extracting(Payment::id).containsExactly(second.id());
        final var cancel1 = cancels.save(cancel(firstOrder, from, CancelType.PARTIAL));
        final var cancel2 = cancels.save(cancel(secondOrder, to, CancelType.ALL));
        cancels.save(cancel(thirdOrder, to.plusNanos(1000), CancelType.ALL));
        assertThat(cancelReader.execute(from, to, 0, 1).content()).extracting(Cancel::id).containsExactly(cancel1.id());
        assertThat(cancelReader.execute(from, to, 0, 1).hasNext()).isTrue();
        assertThat(cancelReader.execute(from, to, 1, 1).content()).extracting(Cancel::id).containsExactly(cancel2.id());
        assertThat(cancelReader.execute(from, to, 1, 1).hasNext()).isFalse();
        assertThat(orderReader.byOrderIds(List.of(firstOrder.id()))).extracting((final var item) -> item.order().id())
            .containsExactly(firstOrder.id());
        assertThat(orderReader.byOrderItemIds(List.of(firstOrder.items().getFirst().id())))
            .extracting((final var item) -> item.order().id()).containsExactly(firstOrder.id());
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
