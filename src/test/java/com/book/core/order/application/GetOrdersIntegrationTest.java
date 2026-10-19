package com.book.core.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.command.GetOrdersCommand;
import com.book.core.order.application.command.OrderListCursor;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.application.result.GetOrdersResult;
import com.book.core.order.application.result.OrderSummaryResult;
import com.book.core.order.application.usecase.GetOrdersUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderStatus;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@Tag("integration")
class GetOrdersIntegrationTest {
    private static final long PRODUCT_ID = 9301L;

    @Autowired
    OrderRepositoryPort orderRepository;

    @Autowired
    GetOrdersUseCase getOrders;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void setUpProduct() {
        jdbc.update("""
                INSERT IGNORE INTO books (id, title, author, publisher, category, published_at)
                VALUES (?, '주문내역 테스트 도서', '저자', '출판사', '소설', '2026-01-01')
                """, PRODUCT_ID);
        jdbc.update("""
                INSERT IGNORE INTO products (id, book_id, name, sale_price, discounted_price, cost_price, stock_quantity)
                VALUES (?, ?, '주문내역 테스트 상품', 20.00, 17.25, 10.00, 10)
                """, PRODUCT_ID, PRODUCT_ID);
    }

    @Test
    void 결제_이력이_있는_주문만_취소_주문을_포함해_최초_결제시각_순으로_조회한다() {
        final long userId = 7101L;
        paidOrder(userId, "history_paid", LocalDateTime.of(2026, 9, 1, 10, 0), OrderStatus.PAID);
        paidOrder(userId, "history_partial", LocalDateTime.of(2026, 9, 2, 10, 0), OrderStatus.PARTIAL_CANCELED);
        final Order canceled = paidOrder(userId, "history_canceled", LocalDateTime.of(2026, 9, 3, 10, 0), OrderStatus.CANCELED);
        final Order created = Order.create(userId, "history_created", null);
        created.addItem(PRODUCT_ID, "미결제 도서", null, "저자", BigDecimal.TEN, BigDecimal.TEN, 1);
        orderRepository.save(created);
        jdbc.update("UPDATE orders SET deleted_at = CURRENT_TIMESTAMP(6) WHERE id = ?",
            paidOrder(userId, "history_deleted", LocalDateTime.of(2026, 9, 4, 10, 0), OrderStatus.PAID).id());
        paidOrder(userId + 1000, "history_other_user", LocalDateTime.of(2026, 9, 5, 10, 0), OrderStatus.PAID);

        final GetOrdersResult all = getOrders.execute(new GetOrdersCommand(userId, null, null, null, null, 20));
        final GetOrdersResult canceledOnly = getOrders.execute(new GetOrdersCommand(userId, OrderStatus.CANCELED, null, null, null, 20));

        assertThat(all.orders()).extracting(OrderSummaryResult::key).containsExactly("history_canceled", "history_partial", "history_paid");
        assertThat(all.nextCursor()).isNull();
        assertThat(canceledOnly.orders()).extracting(OrderSummaryResult::key).containsExactly("history_canceled");
        assertThat(jdbc.queryForObject("SELECT paid_at FROM orders WHERE id = ?", LocalDateTime.class, canceled.id()))
            .isEqualTo(LocalDateTime.of(2026, 9, 3, 10, 0));
    }

    @Test
    void 결제일_필터는_시작일_0시를_포함하고_종료일_다음날_0시를_제외한다() {
        final long userId = 7201L;
        paidOrder(userId, "boundary_before_from", LocalDateTime.of(2026, 8, 31, 23, 59, 59, 999_999_000), OrderStatus.PAID);
        paidOrder(userId, "boundary_from", LocalDateTime.of(2026, 9, 1, 0, 0), OrderStatus.PAID);
        paidOrder(userId, "boundary_to_end", LocalDateTime.of(2026, 9, 30, 23, 59, 59, 999_999_000), OrderStatus.PAID);
        paidOrder(userId, "boundary_after_to", LocalDateTime.of(2026, 10, 1, 0, 0), OrderStatus.PAID);
        final LocalDate from = LocalDate.of(2026, 9, 1);
        final LocalDate to = LocalDate.of(2026, 9, 30);

        assertThat(keys(new GetOrdersCommand(userId, null, from, to, null, 20))).containsExactly("boundary_to_end", "boundary_from");
        assertThat(keys(new GetOrdersCommand(userId, null, from, null, null, 20))).containsExactly("boundary_after_to", "boundary_to_end",
            "boundary_from");
        assertThat(keys(new GetOrdersCommand(userId, null, null, to, null, 20))).containsExactly("boundary_to_end", "boundary_from",
            "boundary_before_from");
    }

    @Test
    void 같은_결제시각을_포함한_커서_페이지를_limit을_바꿔도_중복과_누락_없이_조회한다() {
        final long userId = 7301L;
        final LocalDateTime samePaidAt = LocalDateTime.of(2026, 9, 10, 12, 0, 0, 123_456_000);
        paidOrder(userId, "cursor_1", samePaidAt.minusDays(1), OrderStatus.PAID);
        paidOrder(userId, "cursor_2", samePaidAt, OrderStatus.CANCELED);
        paidOrder(userId, "cursor_3", samePaidAt, OrderStatus.PAID);
        paidOrder(userId, "cursor_4", samePaidAt, OrderStatus.PARTIAL_CANCELED);
        paidOrder(userId, "cursor_5", samePaidAt.plusDays(1), OrderStatus.PAID);
        final LocalDate from = LocalDate.of(2026, 9, 1);

        final GetOrdersResult first = getOrders.execute(new GetOrdersCommand(userId, null, from, null, null, 2));
        final OrderListCursor firstCursor = OrderListCursor.parse(first.nextCursor(), null, from, null);
        final GetOrdersResult second = getOrders.execute(new GetOrdersCommand(userId, null, from, null, firstCursor, 1));
        final OrderListCursor secondCursor = OrderListCursor.parse(second.nextCursor(), null, from, null);
        final GetOrdersResult last = getOrders.execute(new GetOrdersCommand(userId, null, from, null, secondCursor, 100));

        final List<String> pagedKeys = new ArrayList<>();
        List.of(first, second, last).forEach((final var page) -> page.orders().forEach((final var order) -> pagedKeys.add(order.key())));
        assertThat(pagedKeys).containsExactly("cursor_5", "cursor_4", "cursor_3", "cursor_2", "cursor_1");
        assertThat(firstCursor.paidAt()).isEqualTo(samePaidAt);
        assertThat(last.nextCursor()).isNull();
    }

    @Test
    void 대표_도서는_취소된_상품을_포함한_첫_주문상품이고_항목_수는_최초_주문상품_수다() {
        final long userId = 7401L;
        final Order order = Order.create(userId, "representative_order", null);
        order.addItem(PRODUCT_ID, "첫 번째 도서", "https://img.test/first.jpg", "저자", BigDecimal.TEN, BigDecimal.TEN, 1);
        order.addItem(PRODUCT_ID, "두 번째 도서", null, "저자", BigDecimal.TEN, BigDecimal.TEN, 2);
        final Order saved = orderRepository.save(order);
        jdbc.update("UPDATE orders SET paid_at = ?, status = 'PARTIAL_CANCELED' WHERE id = ?", LocalDateTime.of(2026, 9, 1, 9, 0),
            saved.id());
        jdbc.update("UPDATE order_item SET status = 'CANCELED', canceled_quantity = quantity WHERE order_id = ? ORDER BY id LIMIT 1",
            saved.id());

        final OrderSummaryResult summary = getOrders.execute(new GetOrdersCommand(userId, null, null, null, null, 20)).orders().getFirst();

        assertThat(summary.representativeBookName()).isEqualTo("첫 번째 도서");
        assertThat(summary.thumbnailUrl()).isEqualTo("https://img.test/first.jpg");
        assertThat(summary.itemCount()).isEqualTo(2);
        assertThat(summary.totalPrice()).isEqualByComparingTo("30.00");
        assertThat(summary.status()).isEqualTo(OrderStatus.PARTIAL_CANCELED);
        assertThat(summary.paidAt()).isEqualTo(LocalDateTime.of(2026, 9, 1, 9, 0));
    }

    @Test
    void 결제된_주문에_주문상품이_없으면_목록_전체를_정합성_오류로_실패한다() {
        final long userId = 7501L;
        paidOrder(userId, "integrity_valid", LocalDateTime.of(2026, 9, 1, 9, 0), OrderStatus.PAID);
        jdbc.update("""
                INSERT INTO orders (user_id, `key`, total_price, status, paid_at)
                VALUES (?, 'integrity_without_items', 0, 'PAID', '2026-09-02 09:00:00')
                """, userId);

        assertThatThrownBy(() -> getOrders.execute(new GetOrdersCommand(userId, null, null, null, null, 20)))
            .isInstanceOf(CoreException.class).extracting(exception -> ((CoreException) exception).errorCode())
            .isEqualTo(ErrorCode.ORDER_DATA_INCONSISTENT);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10, 20})
    void 주문_수와_무관하게_주문과_주문상품을_두_번의_SQL로_조회한다(final int orderCount) {
        final long userId = 7600L + orderCount;
        for (int index = 0; index < orderCount; index++) {
            paidOrder(userId, "sql_count_" + orderCount + "_" + index, LocalDateTime.of(2026, 9, 1, 0, 0).plusMinutes(index),
                OrderStatus.PAID);
        }
        final Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        final GetOrdersResult result = getOrders.execute(new GetOrdersCommand(userId, null, null, null, null, 20));

        assertThat(result.orders()).hasSize(orderCount);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    private List<String> keys(final GetOrdersCommand command) {
        return getOrders.execute(command).orders().stream().map(OrderSummaryResult::key).toList();
    }

    private Order paidOrder(final long userId, final String key, final LocalDateTime paidAt, final OrderStatus status) {
        final Order order = Order.create(userId, key, null);
        order.addItem(PRODUCT_ID, key + " 도서", null, "저자", new BigDecimal("20.00"), new BigDecimal("15.00"), 1);
        final Order saved = orderRepository.save(order);
        jdbc.update("UPDATE orders SET paid_at = ?, status = ? WHERE id = ?", paidAt, status.name(), saved.id());
        return saved;
    }
}
