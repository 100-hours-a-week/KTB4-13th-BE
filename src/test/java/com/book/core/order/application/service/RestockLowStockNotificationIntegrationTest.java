package com.book.core.order.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.notification.application.command.CreateNotificationCommand;
import com.book.core.notification.application.service.NotificationService;
import com.book.core.order.application.command.CreateOrderCommand;
import com.book.core.order.application.command.CreateOrderItemCommand;
import com.book.core.product.infrastructure.scheduler.ProductRestockScheduler;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles({"test", "mysql-test"})
@Testcontainers
@Tag("integration")
@Tag("mysql-migration")
class RestockLowStockNotificationIntegrationTest {
    private static final AtomicLong NEXT_ID = new AtomicLong(4_300_000L);
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");
    @Autowired
    private ProductRestockScheduler restockScheduler;
    @Autowired
    private OrderService orderService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void 입고된_상품이_다시_부족해지면_장바구니_사용자에게_한_번만_알림을_저장한다() throws Exception {
        final long orderingUserId = insertUser();
        final long cartUserId = insertUser();
        final long outsiderId = insertUser();
        final long productId = insertProduct(2, orderingUserId, cartUserId);
        final long restockId = insertPendingRestock(productId, 18);

        restockScheduler.runDaily();
        restockScheduler.runDaily();

        assertThat(jdbc.queryForObject("SELECT status FROM product_restocks WHERE id = ?", String.class, restockId)).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("SELECT received_at IS NOT NULL FROM product_restocks WHERE id = ?", Boolean.class, restockId))
            .isTrue();
        assertThat(inventory(productId)).containsExactly(20, 0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_restocks WHERE product_id = ?", Integer.class, productId)).isOne();
        assertThat(notificationCount(productId)).isZero();

        orderService.createOrder(new CreateOrderCommand(orderingUserId, List.of(new CreateOrderItemCommand(productId, 18))));
        awaitNotificationCount(productId, 2);

        assertThat(inventory(productId)).containsExactly(20, 18);
        assertThat(notificationUserIds(productId)).containsExactly(orderingUserId, cartUserId);
        assertThat(jdbc.queryForObject("SELECT event_key FROM notifications WHERE user_id = ?", String.class, cartUserId))
            .isEqualTo("cart-low-stock:" + productId + ":1:" + cartUserId);

        notificationService.createAllAsync(List.of(lowStockCommand(productId, cartUserId), lowStockCommand(productId, outsiderId)));
        awaitNotificationCount(productId, 3);

        assertThat(notificationUserIds(productId)).containsExactly(orderingUserId, cartUserId, outsiderId);
    }

    private CreateNotificationCommand lowStockCommand(final long productId, final long userId) {
        return new CreateNotificationCommand(userId, "장바구니 상품 품절 임박", "장바구니에 담아둔 '상품'의 재고가 2권 남았어요.",
            "cart-low-stock:" + productId + ":1:" + userId);
    }

    private long insertUser() {
        final long userId = NEXT_ID.incrementAndGet();
        jdbc.update("INSERT INTO users (id, nickname) VALUES (?, ?)", userId, "restock" + userId);
        jdbc.update("INSERT INTO carts (user_id) VALUES (?)", userId);
        return userId;
    }

    private long insertProduct(final int onHandQuantity, final long... cartUserIds) {
        final long productId = NEXT_ID.incrementAndGet();
        jdbc.update("""
            INSERT INTO books (id, title, author, publisher, category, published_at)
            VALUES (?, '책', '작가', '출판사', '소설', '2026-01-01')
            """, productId);
        jdbc.update("""
            INSERT INTO products (id, book_id, name, sale_price, discounted_price, cost_price)
            VALUES (?, ?, '상품', 20000, 18000, 10000)
            """, productId, productId);
        jdbc.update("INSERT INTO product_inventory (product_id, on_hand_quantity, reorder_point, target_stock) VALUES (?, ?, 3, 20)",
            productId, onHandQuantity);
        for (final long userId : cartUserIds) {
            jdbc.update("INSERT INTO cart_item (cart_id, product_id, quantity) SELECT id, ?, 1 FROM carts WHERE user_id = ?", productId,
                userId);
        }
        return productId;
    }

    private long insertPendingRestock(final long productId, final int quantity) {
        jdbc.update("""
            INSERT INTO product_restocks (product_id, quantity, status, created_at, updated_at)
            VALUES (?, ?, 'PENDING', NOW(6) - INTERVAL 2 DAY, NOW(6) - INTERVAL 2 DAY)
            """, productId, quantity);
        return jdbc.queryForObject("SELECT id FROM product_restocks WHERE product_id = ?", Long.class, productId);
    }

    private List<Integer> inventory(final long productId) {
        return jdbc.queryForObject("SELECT on_hand_quantity, reserved_quantity FROM product_inventory WHERE product_id = ?",
            (final ResultSet resultSet, final int row) -> List.of(resultSet.getInt(1), resultSet.getInt(2)), productId);
    }

    private int notificationCount(final long productId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE event_key LIKE ?", Integer.class,
            "cart-low-stock:" + productId + ":%");
    }

    private List<Long> notificationUserIds(final long productId) {
        return jdbc.queryForList("SELECT user_id FROM notifications WHERE event_key LIKE ? ORDER BY user_id", Long.class,
            "cart-low-stock:" + productId + ":%");
    }

    private void awaitNotificationCount(final long productId, final int expectedCount) throws InterruptedException {
        final long deadline = System.nanoTime() + 5_000_000_000L;
        while (notificationCount(productId) != expectedCount && System.nanoTime() < deadline) {
            Thread.sleep(25);
        }
        assertThat(notificationCount(productId)).isEqualTo(expectedCount);
    }
}
