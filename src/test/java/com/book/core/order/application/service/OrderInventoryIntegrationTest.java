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

        assertThatThrownBy(() -> order(userId, enough, 2, scarce, 2)).isInstanceOfSatisfying(CoreException.class,
            (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INSUFFICIENT_PRODUCT_STOCK));
        assertThat(inventory(enough)).containsExactly(5, 0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE user_id = ?", Integer.class, userId)).isZero();

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
    void 만료된_미결제_주문만_만료하고_결제를_시작한_주문은_재고를_유지한다() {
        final long userId = insertUser();
        final long productId = insertProduct(5, userId);
        final String expired = order(userId, productId, 1);
        final String paying = order(userId, productId, 2);
        expireNow(expired);
        expireNow(paying);
        jdbc.update("""
            INSERT INTO payment (created_at, updated_at, user_id, order_id, origin_amount, owned_coupon_id, coupon_discount,
                used_point, payer_user_id, paid_amount, state, method)
            SELECT NOW(6), NOW(6), user_id, id, total_price, 0, 0, 0, user_id, total_price, 'READY', 'CARD' FROM orders WHERE `key` = ?
            """, paying);

        assertThat(orderService.getExpiredOrderKeys()).contains(expired).doesNotContain(paying);
        orderService.expireOrder(expired);
        orderService.expireOrder(paying);
        orderService.expireOrder(expired);

        assertThat(status(expired)).isEqualTo("EXPIRED");
        assertThat(status(paying)).isEqualTo("CREATED");
        assertThat(inventory(productId)).containsExactly(5, 2);
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
}
