package com.book.core.order.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.application.command.CancelOrderCommand;
import com.book.core.order.application.usecase.CancelOrderUseCase;
import com.book.core.order.application.usecase.GetOrderItemUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderAddress;
import com.book.core.order.domain.OrderItem;
import com.book.core.order.domain.OrderItemStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class OrderRepositoryIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    OrderRepositoryPort orderRepository;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    GetOrderItemUseCase getOrderItem;

    @Autowired
    CancelOrderUseCase cancelOrder;

    @Test
    void 주문_배송지_상품과_상태를_실제_테이블에_저장한다() {
        insertBook(9101L);
        insertProduct(9201L, 9101L);
        final Order order = Order.create(42L, "order_persistence_test", OrderAddress.from("06236", "서울 주소", "101호"));
        order.addItem(9201L, "주문 테스트 상품", null, "저자", new BigDecimal("20.00"), new BigDecimal("17.25"), 2);

        final Order savedOrder = orderRepository.save(order);

        assertThat(savedOrder.id()).isNotNull();
        final Long addressId = jdbc.queryForObject("SELECT address_id FROM orders WHERE `key` = ?", Long.class, "order_persistence_test");
        assertThat(jdbc.queryForMap("SELECT postal_code, address, detail_address FROM order_addresses WHERE id = ?", addressId))
            .containsEntry("postal_code", "06236").containsEntry("address", "서울 주소").containsEntry("detail_address", "101호");
        assertThat(jdbc.queryForMap("SELECT user_id, total_price, status, deleted_at FROM orders WHERE id = ?", savedOrder.id()))
            .containsEntry("user_id", 42L).containsEntry("total_price", new BigDecimal("34.50")).containsEntry("status", "CREATED")
            .containsEntry("deleted_at", null);
        assertThat(jdbc.queryForMap(
            "SELECT product_id, item_name, thumbnail_url, author, sale_price, unit_price, total_price, quantity, status, deleted_at "
                + "FROM order_item WHERE order_id = ?",
            savedOrder.id())).containsEntry("product_id", 9201L).containsEntry("item_name", "주문 테스트 상품")
            .containsEntry("thumbnail_url", null).containsEntry("author", "저자").containsEntry("sale_price", new BigDecimal("20.00"))
            .containsEntry("unit_price", new BigDecimal("17.25")).containsEntry("total_price", new BigDecimal("34.50"))
            .containsEntry("quantity", 2).containsEntry("status", "CREATED").containsEntry("deleted_at", null);
    }

    @Test
    void 배송지가_없는_주문은_NULL_address_id로_저장한다() {
        insertBook(9103L);
        insertProduct(9203L, 9103L);

        final Order savedOrder = orderRepository.save(order("order_without_address", 9203L, null));

        assertThat(savedOrder.id()).isNotNull();
        assertThat(jdbc.queryForMap("SELECT address_id FROM orders WHERE `key` = ?", "order_without_address")).containsEntry("address_id",
            null);
    }

    @Test
    void 주문상품_조회는_활성_주문상품을_상태와_무관하게_반환한다() {
        insertBook(9104L);
        insertProduct(9204L, 9104L);
        final Order savedOrder = orderRepository.save(order("order_item_lookup_test", 9204L, null));
        final Long orderItemId = jdbc.queryForObject("SELECT id FROM order_item WHERE order_id = ?", Long.class, savedOrder.id());

        final OrderItem orderItem = getOrderItem.execute(orderItemId);
        assertThat(orderItem.id()).isEqualTo(orderItemId);
        assertThat(orderItem.status()).isEqualTo(OrderItemStatus.CREATED);

        jdbc.update("UPDATE order_item SET deleted_at = CURRENT_TIMESTAMP(6) WHERE id = ?", orderItemId);
        assertThatThrownBy(() -> getOrderItem.execute(orderItemId)).isInstanceOf(CoreException.class)
            .extracting(exception -> ((CoreException) exception).errorCode()).isEqualTo(ErrorCode.ORDER_ITEM_NOT_FOUND);
    }

    @Test
    void 중복_주문키와_없는_상품_FK는_저장을_거부하고_일부_스냅샷을_남기지_않는다() {
        insertBook(9102L);
        insertProduct(9202L, 9102L);
        final Order firstOrder = order("order_duplicate_test", 9202L, "기준 주소");
        orderRepository.save(firstOrder);

        assertThatThrownBy(() -> orderRepository.save(order("order_duplicate_test", 9202L, "중복 주소")))
            .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE `key` = ?", Integer.class, "order_duplicate_test")).isEqualTo(1);

        assertThatThrownBy(() -> orderRepository.save(order("order_invalid_product", 999999L, "실패 주소")))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE `key` = ?", Integer.class, "order_invalid_product")).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM order_addresses WHERE address = ?", Integer.class, "실패 주소")).isZero();
    }

    @Test
    void 주문_전체_취소는_주문과_모든_주문상품을_같은_트랜잭션에서_저장한다() {
        insertBook(9111L);
        insertProduct(9211L, 9111L);
        final Order order = order("order_cancel_persistence_test", 9211L, null);
        order.addItem(9211L, "주문 테스트 상품 2", null, "저자", new BigDecimal("20.00"), new BigDecimal("7.00"), 2);
        final Order savedOrder = orderRepository.save(order);

        cancelOrder.execute(new CancelOrderCommand(42L, "order_cancel_persistence_test"));

        assertThat(jdbc.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, savedOrder.id())).isEqualTo("CANCELED");
        assertThat(jdbc.queryForObject("SELECT canceled_at IS NOT NULL FROM orders WHERE id = ?", Boolean.class, savedOrder.id())).isTrue();
        assertThat(jdbc.queryForList("SELECT status FROM order_item WHERE order_id = ? ORDER BY id", String.class, savedOrder.id()))
            .containsExactly("CANCELED", "CANCELED");
    }

    @Test
    void 결제된_주문은_취소하지_않고_상태와_취소시각을_유지한다() {
        insertBook(9112L);
        insertProduct(9212L, 9112L);
        final Order savedOrder = orderRepository.save(order("order_paid_cancel_rejected", 9212L, null));
        jdbc.update("UPDATE orders SET status = 'PAID' WHERE id = ?", savedOrder.id());

        assertThatThrownBy(() -> cancelOrder.execute(new CancelOrderCommand(42L, "order_paid_cancel_rejected")))
            .isInstanceOf(CoreException.class).extracting(exception -> ((CoreException) exception).errorCode())
            .isEqualTo(ErrorCode.ORDER_CANNOT_BE_CANCELED);

        assertThat(jdbc.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, savedOrder.id())).isEqualTo("PAID");
        assertThat(jdbc.queryForObject("SELECT canceled_at IS NULL FROM orders WHERE id = ?", Boolean.class, savedOrder.id())).isTrue();
        assertThat(jdbc.queryForObject("SELECT status FROM order_item WHERE order_id = ?", String.class, savedOrder.id()))
            .isEqualTo("CREATED");
    }

    @Test
    void 생성_상태가_아닌_주문상품이_있으면_주문_전체를_변경하지_않는다() {
        insertBook(9114L);
        insertProduct(9214L, 9114L);
        final Order order = order("order_paid_item_cancel_rejected", 9214L, null);
        order.addItem(9214L, "주문 테스트 상품 2", null, "저자", new BigDecimal("20.00"), new BigDecimal("7.00"), 2);
        final Order savedOrder = orderRepository.save(order);
        final Long firstItemId =
            jdbc.queryForObject("SELECT id FROM order_item WHERE order_id = ? ORDER BY id LIMIT 1", Long.class, savedOrder.id());
        jdbc.update("UPDATE order_item SET status = 'PAID' WHERE id = ?", firstItemId);

        assertThatThrownBy(() -> cancelOrder.execute(new CancelOrderCommand(42L, "order_paid_item_cancel_rejected")))
            .isInstanceOf(CoreException.class).extracting(exception -> ((CoreException) exception).errorCode())
            .isEqualTo(ErrorCode.ORDER_CANNOT_BE_CANCELED);

        assertThat(jdbc.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, savedOrder.id())).isEqualTo("CREATED");
        assertThat(jdbc.queryForObject("SELECT canceled_at IS NULL FROM orders WHERE id = ?", Boolean.class, savedOrder.id())).isTrue();
        assertThat(jdbc.queryForList("SELECT status FROM order_item WHERE order_id = ? ORDER BY id", String.class, savedOrder.id()))
            .containsExactly("PAID", "CREATED");
    }

    @Test
    void 동시에_취소하면_한_요청만_성공하고_다른_요청은_상태_충돌로_실패한다() throws Exception {
        insertBook(9113L);
        insertProduct(9213L, 9113L);
        orderRepository.save(order("order_concurrent_cancel", 9213L, null));
        final CancelOrderCommand command = new CancelOrderCommand(42L, "order_concurrent_cancel");
        final var ready = new CountDownLatch(2);
        final var start = new CountDownLatch(1);
        final var executor = Executors.newFixedThreadPool(2);

        try {
            final var first = executor.submit(() -> cancelAfterStart(command, ready, start));
            final var second = executor.submit(() -> cancelAfterStart(command, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void 배송지와_상품_FK_및_주문키_UNIQUE_제약을_마이그레이션에_생성한다() {
        assertThat(countKeyUsage("orders", "address_id", "order_addresses", "id")).isEqualTo(1);
        assertThat(countKeyUsage("order_item", "product_id", "products", "id")).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                        SELECT COUNT(*)
                        FROM information_schema.statistics
                        WHERE table_schema = DATABASE()
                          AND table_name = 'orders'
                          AND index_name = 'uk_orders_key'
                          AND non_unique = 0
                        """, Integer.class)).isEqualTo(1);
        assertThat(countColumn("orders", "deleted_at")).isEqualTo(1);
        assertThat(countColumn("order_item", "deleted_at")).isEqualTo(1);
    }

    private Order order(final String orderKey, final long productId, final String address) {
        final OrderAddress orderAddress = address == null ? null : OrderAddress.from("06236", address, null);
        final Order order = Order.create(42L, orderKey, orderAddress);
        order.addItem(productId, "주문 테스트 상품", null, "저자", new BigDecimal("20.00"), new BigDecimal("5.00"), 1);
        return order;
    }

    private int countKeyUsage(final String table, final String column, final String referencedTable, final String referencedColumn) {
        return jdbc.queryForObject("""
                        SELECT COUNT(*)
                        FROM information_schema.key_column_usage
                        WHERE constraint_schema = DATABASE()
                          AND table_name = ?
                          AND column_name = ?
                          AND referenced_table_name = ?
                          AND referenced_column_name = ?
                        """, Integer.class, table, column, referencedTable, referencedColumn);
    }

    private int countColumn(final String table, final String column) {
        return jdbc.queryForObject("""
                        SELECT COUNT(*)
                        FROM information_schema.columns
                        WHERE table_schema = DATABASE()
                          AND table_name = ?
                          AND column_name = ?
                        """, Integer.class, table, column);
    }

    private boolean cancelAfterStart(final CancelOrderCommand command, final CountDownLatch ready, final CountDownLatch start)
        throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            cancelOrder.execute(command);
            return true;
        } catch (final CoreException exception) {
            if (exception.errorCode() == ErrorCode.ORDER_CANNOT_BE_CANCELED) {
                return false;
            }
            throw exception;
        }
    }

    private void insertBook(final long id) {
        jdbc.update("""
                INSERT INTO books (id, title, author, publisher, category, published_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, "주문 테스트 도서", "저자", "출판사", "소설", "2026-01-01");
    }

    private void insertProduct(final long id, final long bookId) {
        jdbc.update("""
                INSERT INTO products (id, book_id, name, sale_price, discounted_price, cost_price, stock_quantity)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, id, bookId, "주문 테스트 상품", new BigDecimal("20.00"), new BigDecimal("17.25"), new BigDecimal("10.00"), 10);
    }
}
