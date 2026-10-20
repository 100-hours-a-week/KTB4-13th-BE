package com.book.core.order.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.command.CancelOrderCommand;
import com.book.core.order.application.command.CreateOrderCommand;
import com.book.core.order.application.command.CreateOrderItemCommand;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
class OrderInventoryIntegrationTest {
    private static final AtomicLong NEXT_ID = new AtomicLong(4_200_000L);
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");
    @Autowired
    private OrderService orderService;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void 주문을_만들면_예약하고_한_상품이라도_부족하면_주문과_예약을_모두_롤백한다() {
        final long userId = insertUser();
        final long enough = insertProduct(5, userId);
        final long scarce = insertProduct(1, userId);

        assertThatThrownBy(() -> order(userId, enough, 3, scarce, 2)).isInstanceOfSatisfying(CoreException.class,
            (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INSUFFICIENT_PRODUCT_STOCK));
        assertThat(inventory(enough)).containsExactly(5, 0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE user_id = ?", Integer.class, userId)).isZero();
        assertThat(notificationCount(enough)).isZero();

        final String orderKey = order(userId, enough, 2, scarce, 1);

        assertThat(inventory(enough)).containsExactly(5, 2);
        assertThat(inventory(scarce)).containsExactly(1, 1);
        assertThat(jdbc.queryForObject("SELECT expires_at > created_at FROM orders WHERE `key` = ?", Boolean.class, orderKey)).isTrue();
    }

    @Test
    void 동시에_주문해도_판매_가능_수량만큼만_주문된다() throws Exception {
        final long productId = insertProduct(3);
        final CountDownLatch start = new CountDownLatch(1);
        final List<Future<Boolean>> results = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
            for (int index = 0; index < 8; index++) {
                final long userId = insertUser();
                addToCart(userId, productId);
                results.add(executor.submit((Callable<Boolean>) () -> {
                    start.await();
                    try {
                        order(userId, productId, 1);
                        return true;
                    } catch (final CoreException exception) {
                        return false;
                    }
                }));
            }
            start.countDown();
            int ordered = 0;
            for (final Future<Boolean> result : results) {
                if (result.get()) {
                    ordered++;
                }
            }
            assertThat(ordered).isEqualTo(3);
        }
        assertThat(inventory(productId)).containsExactly(3, 3);
        assertThat(jdbc.queryForObject("SELECT low_stock_cycle FROM product_inventory WHERE product_id = ?", Long.class, productId))
            .isEqualTo(1);
    }

    @Test
    void 부족_진입_당시_장바구니_사용자에게만_주기마다_한_번_알림을_보낸다() throws Exception {
        final long orderingUserId = insertUser();
        final long cartUserId = insertUser();
        final long userWithoutCartItem = insertUser();
        final long productId = insertProduct(5, orderingUserId, cartUserId);

        final String firstOrderKey = order(orderingUserId, productId, 3);
        awaitNotificationCount(productId, 2);

        assertThat(notificationUserIds(productId)).containsExactlyInAnyOrder(orderingUserId, cartUserId)
            .doesNotContain(userWithoutCartItem);
        assertThat(
            jdbc.queryForList("SELECT title FROM notifications WHERE event_key LIKE ?", String.class, "cart-low-stock:" + productId + ":%"))
            .containsOnly("장바구니 상품 품절 임박");
        assertThat(
            jdbc.queryForList("SELECT body FROM notifications WHERE event_key LIKE ?", String.class, "cart-low-stock:" + productId + ":%"))
            .containsOnly("장바구니에 담아둔 '상품'의 재고가 2권 남았어요.");

        final String secondOrderKey = order(orderingUserId, productId, 1);
        assertThat(notificationCount(productId)).isEqualTo(2);

        orderService.cancelOrder(new CancelOrderCommand(orderingUserId, firstOrderKey));
        orderService.cancelOrder(new CancelOrderCommand(orderingUserId, secondOrderKey));
        order(orderingUserId, productId, 3);
        awaitNotificationCount(productId, 4);

        assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT event_key) FROM notifications WHERE event_key LIKE ?", Integer.class,
            "cart-low-stock:" + productId + ":%")).isEqualTo(4);
    }

    @Test
    void 재고가_5개에서_0개가_되면_알림을_보내지_않는다() throws Exception {
        final long userId = insertUser();
        final long productId = insertProduct(5, userId);

        order(userId, productId, 5);
        Thread.sleep(100);

        assertThat(notificationCount(productId)).isZero();
    }

    @Test
    void 결제_전_취소는_예약을_한_번만_반환하고_만료와_겹쳐도_다시_반환하지_않는다() {
        final long userId = insertUser();
        final long productId = insertProduct(5, userId);
        final String orderKey = order(userId, productId, 2);

        orderService.cancelOrder(new CancelOrderCommand(userId, orderKey));
        expireNow(orderKey);
        orderService.expireOrder(orderKey);

        assertThatThrownBy(() -> orderService.cancelOrder(new CancelOrderCommand(userId, orderKey))).isInstanceOf(CoreException.class);
        assertThat(inventory(productId)).containsExactly(5, 0);
        assertThat(status(orderKey)).isEqualTo("CANCELED");
    }

    @Test
    void PG_승인이_진행_중인_주문만_예약을_유지하고_승인_전이거나_거절된_주문은_만료한다() {
        final long userId = insertUser();
        final long productId = insertProduct(10, userId);
        final String notStarted = order(userId, productId, 1);
        final String pending = order(userId, productId, 2);
        final String rejected = order(userId, productId, 3);
        List.of(notStarted, pending, rejected).forEach(this::expireNow);
        insertPayment(notStarted);
        insertPgOperation(pending, false);
        insertPgOperation(rejected, true);

        assertThat(orderService.getExpiredOrderKeys()).contains(notStarted, rejected).doesNotContain(pending);
        List.of(notStarted, pending, rejected, notStarted).forEach(orderService::expireOrder);

        assertThat(status(notStarted)).isEqualTo("EXPIRED");
        assertThat(status(pending)).isEqualTo("CREATED");
        assertThat(status(rejected)).isEqualTo("EXPIRED");
        assertThat(inventory(productId)).containsExactly(10, 2);
    }

    @Test
    void 예약_없이_만든_기존_주문을_취소해도_예약_재고를_바꾸지_않는다() {
        final long userId = insertUser();
        final long productId = insertProduct(5, userId);
        final String legacy = order(userId, productId, 1);
        jdbc.update("UPDATE orders SET expires_at = NULL WHERE `key` = ?", legacy);
        jdbc.update("UPDATE product_inventory SET reserved_quantity = 0 WHERE product_id = ?", productId);

        orderService.cancelOrder(new CancelOrderCommand(userId, legacy));

        assertThat(status(legacy)).isEqualTo("CANCELED");
        assertThat(inventory(productId)).containsExactly(5, 0);
    }

    private String order(final long userId, final Object... productQuantities) {
        final List<CreateOrderItemCommand> items = new ArrayList<>();
        for (int index = 0; index < productQuantities.length; index += 2) {
            items.add(new CreateOrderItemCommand((Long) productQuantities[index], (Integer) productQuantities[index + 1]));
        }
        return orderService.createOrder(new CreateOrderCommand(userId, items)).orderKey();
    }

    private long insertUser() {
        final long userId = NEXT_ID.incrementAndGet();
        jdbc.update("INSERT INTO users (id, nickname) VALUES (?, ?)", userId, "inventory" + userId);
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
        jdbc.update("INSERT INTO product_inventory (product_id, on_hand_quantity) VALUES (?, ?)", productId, onHandQuantity);
        for (final long userId : cartUserIds) {
            addToCart(userId, productId);
        }
        return productId;
    }

    private void addToCart(final long userId, final long productId) {
        jdbc.update("INSERT INTO cart_item (cart_id, product_id, quantity) SELECT id, ?, 1 FROM carts WHERE user_id = ?", productId,
            userId);
    }

    private void insertPayment(final String orderKey) {
        jdbc.update("""
            INSERT INTO payment (created_at, updated_at, user_id, order_id, origin_amount, owned_coupon_id, coupon_discount,
                used_point, payer_user_id, paid_amount, state, method)
            SELECT NOW(6), NOW(6), user_id, id, total_price, 0, 0, 0, user_id, total_price, 'READY', 'CARD' FROM orders WHERE `key` = ?
            """, orderKey);
    }

    private void insertPgOperation(final String orderKey, final boolean rejected) {
        jdbc.update("""
            INSERT INTO pg_operation (order_key, request_hash, input_hash, pg_idempotency_key, type, requested_at, completed, rejected,
                created_at, updated_at)
            VALUES (?, ?, 'input', UUID(), 'APPROVE', NOW(6), 0, ?, NOW(6), NOW(6))
            """, orderKey, "request-" + orderKey, rejected);
    }

    private void expireNow(final String orderKey) {
        jdbc.update("UPDATE orders SET expires_at = '2020-01-01 00:00:00' WHERE `key` = ?", orderKey);
    }

    private String status(final String orderKey) {
        return jdbc.queryForObject("SELECT status FROM orders WHERE `key` = ?", String.class, orderKey);
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
