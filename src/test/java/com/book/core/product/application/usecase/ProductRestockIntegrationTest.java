package com.book.core.product.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.core.product.application.service.ProductService;
import com.book.core.product.infrastructure.scheduler.ProductInventoryScheduler;
import java.util.ArrayList;
import java.util.List;
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
class ProductRestockIntegrationTest {
    private static final AtomicLong NEXT_ID = new AtomicLong(4_300_000L);
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");
    @Autowired
    private ProductService productService;
    @Autowired
    private ProductInventoryScheduler scheduler;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void 보유_재고가_발주_기준_이하인_상품만_목표_재고까지_한_번_발주한다() {
        final long low = insertProduct(3);
        final long enough = insertProduct(4);

        assertThat(productService.getRestockCandidates()).contains(low).doesNotContain(enough);
        assertThat(productService.createRestock(low)).isTrue();
        assertThat(productService.createRestock(low)).isFalse();
        assertThat(productService.createRestock(enough)).isFalse();

        assertThat(productService.getRestockCandidates()).doesNotContain(low);
        assertThat(
            jdbc.queryForList("SELECT quantity FROM product_restocks WHERE product_id = ? AND status = 'PENDING'", Integer.class, low))
            .containsExactly(17);
    }

    @Test
    void 동시에_발주해도_상품별_대기_발주는_하나다() throws Exception {
        final long productId = insertProduct(0);
        final CountDownLatch start = new CountDownLatch(1);
        final List<Future<Boolean>> results = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
            for (int index = 0; index < 8; index++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return productService.createRestock(productId);
                }));
            }
            start.countDown();
            for (final Future<Boolean> result : results) {
                result.get();
            }
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_restocks WHERE product_id = ?", Integer.class, productId))
            .isEqualTo(1);
    }

    @Test
    void 생성_후_24시간이_지난_발주만_한_번_입고하고_보유_재고를_늘린다() {
        final long received = insertProduct(2);
        final long recent = insertProduct(1);
        productService.createRestock(received);
        productService.createRestock(recent);
        jdbc.update("UPDATE product_restocks SET created_at = NOW(6) - INTERVAL 25 HOUR WHERE product_id = ?", received);

        scheduler.runDaily();

        assertThat(productService.receiveRestock(restockId(received))).isFalse();
        assertThat(status(received)).isEqualTo("COMPLETED");
        assertThat(onHand(received)).isEqualTo(20);
        assertThat(status(recent)).isEqualTo("PENDING");
        assertThat(onHand(recent)).isEqualTo(1);
    }

    @Test
    void 보유_재고를_늘리지_못하면_입고_완료도_롤백한다() {
        final long productId = insertProduct(1);
        productService.createRestock(productId);
        jdbc.update("DELETE FROM product_inventory WHERE product_id = ?", productId);

        assertThatThrownBy(() -> productService.receiveRestock(restockId(productId))).isInstanceOf(CoreException.class);

        assertThat(status(productId)).isEqualTo("PENDING");
    }

    private long insertProduct(final int onHandQuantity) {
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
        return productId;
    }

    private long restockId(final long productId) {
        return jdbc.queryForObject("SELECT id FROM product_restocks WHERE product_id = ?", Long.class, productId);
    }

    private String status(final long productId) {
        return jdbc.queryForObject("SELECT status FROM product_restocks WHERE product_id = ?", String.class, productId);
    }

    private int onHand(final long productId) {
        return jdbc.queryForObject("SELECT on_hand_quantity FROM product_inventory WHERE product_id = ?", Integer.class, productId);
    }
}
