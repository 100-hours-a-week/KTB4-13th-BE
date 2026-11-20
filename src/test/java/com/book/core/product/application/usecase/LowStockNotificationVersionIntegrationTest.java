package com.book.core.product.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.product.application.command.ProductQuantitiesCommand;
import com.book.core.product.application.port.ProductInventoryRepositoryPort;
import com.book.core.product.application.result.ProductInventoryState;
import com.book.core.product.application.result.LowStockInventoryEntry;
import java.time.LocalDateTime;
import java.util.Map;
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
class LowStockNotificationVersionIntegrationTest {
    private static final AtomicLong NEXT_ID = new AtomicLong(4_900_000L);

    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    ReserveProductInventoryUseCase reserve;

    @Autowired
    ReleaseProductInventoryUseCase release;

    @Autowired
    RestoreProductInventoryUseCase restore;

    @Autowired
    CompleteProductInventoryUseCase complete;

    @Autowired
    ReceiveProductRestockUseCase receiveRestock;

    @Autowired
    ProductInventoryRepositoryPort inventoryRepository;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 예약_반환으로_5개를_초과하면_이전_진입_버전이_무효화된다() {
        final long productId = insertProduct(6);
        final ProductInventoryState entered = reserveOne(productId).get(productId).state();
        assertThat(entered).isEqualTo(new ProductInventoryState(5, 0));

        release.execute(one(productId));

        assertThat(state(productId)).isEqualTo(new ProductInventoryState(6, 1));
        assertThat(reserveOne(productId).get(productId).state()).isEqualTo(new ProductInventoryState(5, 1));
    }

    @Test
    void 재고_복원으로_5개를_초과할_때만_버전이_증가한다() {
        final long productId = insertProduct(5);

        restore.execute(one(productId));
        assertThat(state(productId)).isEqualTo(new ProductInventoryState(6, 1));
        restore.execute(one(productId));
        assertThat(state(productId)).isEqualTo(new ProductInventoryState(7, 1));
    }

    @Test
    void 완료된_입고는_재고가_여전히_5개_이하여도_버전을_증가시킨다() {
        final long productId = insertProduct(3);
        jdbc.update("INSERT INTO product_restocks (product_id, quantity, status) VALUES (?, 1, 'PENDING')", productId);
        final Long restockId = jdbc.queryForObject("SELECT id FROM product_restocks WHERE product_id = ?", Long.class, productId);

        assertThat(receiveRestock.execute(restockId, LocalDateTime.of(2026, 10, 26, 12, 0))).isTrue();
        assertThat(state(productId)).isEqualTo(new ProductInventoryState(4, 1));
        assertThat(receiveRestock.execute(restockId, LocalDateTime.of(2026, 10, 26, 12, 1))).isFalse();
        assertThat(state(productId)).isEqualTo(new ProductInventoryState(4, 1));
    }

    @Test
    void 단순_추가_예약과_예약_완료는_버전을_바꾸지_않는다() {
        final long productId = insertProduct(6);

        assertThat(reserveOne(productId).get(productId).state()).isEqualTo(new ProductInventoryState(5, 0));
        assertThat(reserveOne(productId)).isEmpty();
        complete.execute(one(productId));

        assertThat(state(productId)).isEqualTo(new ProductInventoryState(4, 0));
    }

    @Test
    void 동시_예약_반환으로_5개를_초과해도_버전은_한_번만_증가한다() throws Exception {
        final long productId = insertProduct(6);
        reserve.executeWithLowStockNotificationEntries(new ProductQuantitiesCommand(Map.of(productId, 2)), 5);
        final CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            final Future<?> first = executor.submit(() -> releaseAfterStart(productId, start));
            final Future<?> second = executor.submit(() -> releaseAfterStart(productId, start));
            start.countDown();
            first.get();
            second.get();
        }

        assertThat(state(productId)).isEqualTo(new ProductInventoryState(6, 1));
    }

    @Test
    void 여러_상품_중_예약이_실패하면_수량과_버전이_모두_롤백된다() {
        final long availableId = insertProduct(6);
        final long unavailableId = insertProduct(0);

        assertThatThrownBy(
            () -> reserve.executeWithLowStockNotificationEntries(new ProductQuantitiesCommand(Map.of(availableId, 1, unavailableId, 1)), 5))
            .isInstanceOfSatisfying(CoreException.class,
                exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INSUFFICIENT_PRODUCT_STOCK));
        assertThat(state(availableId)).isEqualTo(new ProductInventoryState(6, 0));
        assertThat(state(unavailableId)).isEqualTo(new ProductInventoryState(0, 0));
    }

    private void releaseAfterStart(final long productId, final CountDownLatch start) {
        try {
            start.await();
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
        release.execute(one(productId));
    }

    private Map<Long, LowStockInventoryEntry> reserveOne(final long productId) {
        return reserve.executeWithLowStockNotificationEntries(one(productId), 5);
    }

    private ProductInventoryState state(final long productId) {
        return inventoryRepository.state(productId);
    }

    private ProductQuantitiesCommand one(final long productId) {
        return new ProductQuantitiesCommand(Map.of(productId, 1));
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
}
