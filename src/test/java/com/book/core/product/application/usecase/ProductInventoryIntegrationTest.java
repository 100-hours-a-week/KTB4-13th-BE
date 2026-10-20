package com.book.core.product.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.product.application.command.ProductQuantitiesCommand;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
class ProductInventoryIntegrationTest {
    private static final AtomicLong NEXT_ID = new AtomicLong(4_100_000L);
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");
    @Autowired
    private ReserveProductInventoryUseCase reserve;
    @Autowired
    private ReleaseProductInventoryUseCase release;
    @Autowired
    private CompleteProductInventoryUseCase complete;
    @Autowired
    private RestoreProductInventoryUseCase restore;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void 예약_결제_확정_취소_복구는_예약과_보유_재고를_정해진_만큼만_바꾼다() {
        final long productId = insertProduct(10);

        reserve.execute(quantities(productId, 4));
        assertThat(inventory(productId)).containsExactly(10, 4);
        release.execute(quantities(productId, 1));
        assertThat(inventory(productId)).containsExactly(10, 3);
        complete.execute(quantities(productId, 3));
        assertThat(inventory(productId)).containsExactly(7, 0);
        restore.execute(quantities(productId, 2));
        assertThat(inventory(productId)).containsExactly(9, 0);
    }

    @Test
    void 한_상품이라도_조건을_만족하지_못하면_모든_상품의_변경을_롤백한다() {
        final long first = insertProduct(5);
        final long second = insertProduct(1);

        assertError(() -> reserve.execute(new ProductQuantitiesCommand(Map.of(first, 2, second, 2))), ErrorCode.INSUFFICIENT_PRODUCT_STOCK);
        assertError(() -> release.execute(quantities(first, 1)), ErrorCode.ORDER_DATA_INCONSISTENT);
        assertError(() -> complete.execute(quantities(first, 1)), ErrorCode.ORDER_DATA_INCONSISTENT);
        assertError(() -> restore.execute(quantities(NEXT_ID.incrementAndGet(), 1)), ErrorCode.ORDER_DATA_INCONSISTENT);

        assertThat(inventory(first)).containsExactly(5, 0);
        assertThat(inventory(second)).containsExactly(1, 0);
    }

    @Test
    void 동시에_예약해도_판매_가능_수량을_넘지_않는다() throws Exception {
        final long productId = insertProduct(5);
        final CountDownLatch start = new CountDownLatch(1);
        final List<Future<Boolean>> results = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(10)) {
            for (int index = 0; index < 20; index++) {
                results.add(executor.submit((Callable<Boolean>) () -> {
                    start.await();
                    try {
                        reserve.execute(quantities(productId, 1));
                        return true;
                    } catch (final CoreException exception) {
                        return false;
                    }
                }));
            }
            start.countDown();
            int reserved = 0;
            for (final Future<Boolean> result : results) {
                if (result.get()) {
                    reserved++;
                }
            }
            assertThat(reserved).isEqualTo(5);
        }
        assertThat(inventory(productId)).containsExactly(5, 5);
    }

    @Test
    void 재고_행이_없는_상품은_기본_재고로_예약한다() {
        final long productId = NEXT_ID.incrementAndGet();
        insertBookAndProduct(productId);

        reserve.execute(quantities(productId, 2));

        assertThat(jdbc.queryForMap(
            "SELECT on_hand_quantity, reserved_quantity, reorder_point, target_stock FROM product_inventory " + "WHERE product_id = ?",
            productId)).containsEntry("on_hand_quantity", 20).containsEntry("reserved_quantity", 2).containsEntry("reorder_point", 3)
            .containsEntry("target_stock", 20);
    }

    private long insertProduct(final int onHandQuantity) {
        final long productId = NEXT_ID.incrementAndGet();
        insertBookAndProduct(productId);
        jdbc.update("INSERT INTO product_inventory (product_id, on_hand_quantity) VALUES (?, ?)", productId, onHandQuantity);
        return productId;
    }

    private void insertBookAndProduct(final long productId) {
        jdbc.update("""
            INSERT INTO books (id, title, author, publisher, category, published_at)
            VALUES (?, '책', '작가', '출판사', '소설', '2026-01-01')
            """, productId);
        jdbc.update("""
            INSERT INTO products (id, book_id, name, sale_price, discounted_price, cost_price)
            VALUES (?, ?, '상품', 20000, 18000, 10000)
            """, productId, productId);
    }

    private List<Integer> inventory(final long productId) {
        return jdbc.queryForObject("SELECT on_hand_quantity, reserved_quantity FROM product_inventory WHERE product_id = ?",
            (final ResultSet resultSet, final int row) -> List.of(resultSet.getInt(1), resultSet.getInt(2)), productId);
    }

    private static ProductQuantitiesCommand quantities(final long productId, final int quantity) {
        return new ProductQuantitiesCommand(Map.of(productId, quantity));
    }

    private static void assertError(final Runnable call, final ErrorCode errorCode) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(CoreException.class,
            (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(errorCode));
    }
}
