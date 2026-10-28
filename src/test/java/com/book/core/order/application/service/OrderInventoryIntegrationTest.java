package com.book.core.order.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.command.CancelOrderCommand;
import com.book.core.order.application.command.CreateOrderCommand;
import com.book.core.order.application.command.CreateOrderItemCommand;
import com.book.core.product.infrastructure.scheduler.ProductRestockScheduler;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    private ProductRestockScheduler restockScheduler;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void 주문을_만들면_예약하고_한_상품이라도_부족하면_주문과_예약과_알림을_모두_롤백한다() throws Exception {
        final long userId = insertUser();
        final long cartUserId = insertUser();
        final long enough = insertProduct(5, userId, cartUserId);
        final long scarce = insertProduct(1, userId);

        assertThatThrownBy(() -> order(userId, enough, 2, scarce, 2)).isInstanceOfSatisfying(CoreException.class,
            (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INSUFFICIENT_PRODUCT_STOCK));
        assertThat(inventory(enough)).containsExactly(5, 0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE user_id = ?", Integer.class, userId)).isZero();
        Thread.sleep(100);
        assertThat(notificationCount(userId, cartUserId)).isZero();

        final String orderKey = order(userId, enough, 2, scarce, 1);

        assertThat(inventory(enough)).containsExactly(5, 2);
        assertThat(inventory(scarce)).containsExactly(1, 1);
        awaitNotificationCount(1, userId, cartUserId);
        assertThat(notifiedUserIds(userId, cartUserId)).containsExactly(cartUserId);
        assertThat(jdbc.queryForObject("SELECT expires_at > created_at FROM orders WHERE `key` = ?", Boolean.class, orderKey)).isTrue();
    }

    @Test
    void 동시에_주문해도_판매_가능_수량만큼만_주문되고_진입시킨_주문자를_뺀_장바구니_사용자에게_알림을_한_번씩_저장한다() throws Exception {
        final long productId = insertProduct(5);
        final long[] userIds = new long[8];
        final CountDownLatch start = new CountDownLatch(1);
        final List<Future<Boolean>> results = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
            for (int index = 0; index < 8; index++) {
                final long userId = insertUser();
                userIds[index] = userId;
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
            assertThat(ordered).isEqualTo(5);
        }
        assertThat(inventory(productId)).containsExactly(5, 5);
        awaitNotificationCount(7, userIds);
        Thread.sleep(100);
        assertThat(notifiedUserIds(userIds)).hasSize(7).doesNotHaveDuplicates();
    }

    @Test
    void 판매_가능_재고가_3개_이하로_처음_내려갈_때만_주문자를_뺀_장바구니_사용자에게_알리고_입고로_회복된_뒤_다시_내려가면_다시_알린다() throws Exception {
        final long orderingUserId = insertUser();
        final long cartUserId = insertUser();
        final long userWithoutCartItem = insertUser();
        final long productId = insertProduct(5, orderingUserId, cartUserId);

        order(orderingUserId, productId, 2);
        awaitNotificationCount(1, orderingUserId, cartUserId, userWithoutCartItem);
        order(orderingUserId, productId, 1);
        jdbc.update("""
            INSERT INTO product_restocks (product_id, quantity, status, created_at, updated_at)
            VALUES (?, 8, 'PENDING', NOW(6) - INTERVAL 2 DAY, NOW(6) - INTERVAL 2 DAY)
            """, productId);
        restockScheduler.runDaily();
        restockScheduler.runDaily();
        assertThat(inventory(productId)).containsExactly(13, 3);
        order(cartUserId, productId, 7);

        awaitNotificationCount(2, orderingUserId, cartUserId, userWithoutCartItem);
        Thread.sleep(100);
        assertThat(notifiedUserIds(orderingUserId, cartUserId, userWithoutCartItem)).containsExactly(orderingUserId, cartUserId);
        assertThat(jdbc.queryForList("SELECT title FROM notifications WHERE user_id IN (?, ?)", String.class, orderingUserId, cartUserId))
            .containsOnly("장바구니 상품 품절 임박");
        assertThat(jdbc.queryForList("SELECT body FROM notifications WHERE user_id IN (?, ?)", String.class, orderingUserId, cartUserId))
            .containsOnly("장바구니에 담아둔 '상품'의 재고가 3권 남았어요.");
    }

    @Test
    void 판매_가능_재고가_3개_초과에서_0개가_되면_주문자를_뺀_장바구니_사용자에게_품절_알림을_저장한다() throws Exception {
        final long userId = insertUser();
        final long cartUserId = insertUser();
        final long productId = insertProduct(5, userId, cartUserId);

        order(userId, productId, 5);

        awaitNotificationCount(1, userId, cartUserId);
        Thread.sleep(100);
        assertThat(notifiedUserIds(userId, cartUserId)).containsExactly(cartUserId);
        assertThat(jdbc.queryForMap("SELECT title, body FROM notifications WHERE user_id = ?", cartUserId))
            .containsEntry("title", "장바구니 상품 품절").containsEntry("body", "장바구니에 담아둔 '상품'의 재고가 모두 소진됐어요.");
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
    void PG_결과가_확인되지_않은_READY_결제와_진행중인_주문은_예약을_유지한다() {
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

        assertThat(status(notStarted)).isEqualTo("CREATED");
        assertThat(status(pending)).isEqualTo("CREATED");
        assertThat(status(rejected)).isEqualTo("EXPIRED");
        assertThat(inventory(productId)).containsExactly(10, 3);
        assertThat(
            jdbc.queryForObject("SELECT COUNT(*) FROM pg_operation WHERE order_key = ? AND review_required = 1", Integer.class, notStarted))
            .isEqualTo(1);
    }

    @Test
    void 이전_시도의_격리된_PG확인은_현재_주문의_취소와_만료를_막지_않는다() {
        final long userId = insertUser();
        final long productId = insertProduct(5, userId);
        final String canceled = order(userId, productId, 1);
        final String expired = order(userId, productId, 1);
        insertPgOperation(canceled, false);
        insertPgOperation(expired, false);
        jdbc.update("UPDATE pg_operation SET blocks_order = 0, review_required = 1 WHERE order_key IN (?, ?)", canceled, expired);
        expireNow(expired);

        assertThat(orderService.getExpiredOrderKeys()).contains(expired);
        orderService.cancelOrder(new CancelOrderCommand(userId, canceled));
        orderService.expireOrder(expired);

        assertThat(status(canceled)).isEqualTo("CANCELED");
        assertThat(status(expired)).isEqualTo("EXPIRED");
        assertThat(inventory(productId)).containsExactly(5, 0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pg_operation WHERE order_key IN (?, ?) AND review_required = 1", Integer.class,
            canceled, expired)).isEqualTo(2);
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

    private int notificationCount(final long... userIds) {
        return notifiedUserIds(userIds).size();
    }

    private List<Long> notifiedUserIds(final long... userIds) {
        final String placeholders = String.join(",", Collections.nCopies(userIds.length, "?"));
        return jdbc.queryForList("SELECT user_id FROM notifications WHERE user_id IN (" + placeholders + ") ORDER BY user_id, id",
            Long.class, Arrays.stream(userIds).boxed().toArray());
    }

    private void awaitNotificationCount(final int expectedCount, final long... userIds) throws InterruptedException {
        final long deadline = System.nanoTime() + 5_000_000_000L;
        while (notificationCount(userIds) != expectedCount && System.nanoTime() < deadline) {
            Thread.sleep(25);
        }
        assertThat(notificationCount(userIds)).isEqualTo(expectedCount);
    }

    private List<Integer> inventory(final long productId) {
        return jdbc.queryForObject("SELECT on_hand_quantity, reserved_quantity FROM product_inventory WHERE product_id = ?",
            (final ResultSet resultSet, final int row) -> List.of(resultSet.getInt(1), resultSet.getInt(2)), productId);
    }
}
