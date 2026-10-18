package com.book.core.cart.infrastructure.persistence.repository;

import com.book.core.cart.application.command.AddCartItemCommand;
import com.book.core.cart.application.command.ApplyCartChangesCommand;
import com.book.core.cart.application.command.CartItemChange;
import com.book.core.cart.application.command.DeleteCartItemCommand;
import com.book.core.cart.application.command.DeleteCartItemsCommand;
import com.book.core.cart.application.command.ModifyCartItemCommand;
import com.book.core.cart.application.port.CartItemRepositoryPort;
import com.book.core.cart.application.port.CartRepositoryPort;
import com.book.core.cart.application.usecase.AddCartItemUseCase;
import com.book.core.cart.application.usecase.ApplyCartChangesUseCase;
import com.book.core.cart.application.usecase.DeleteCartItemUseCase;
import com.book.core.cart.application.usecase.DeleteCartItemsByProductsUseCase;
import com.book.core.cart.application.usecase.DeleteCartItemsUseCase;
import com.book.core.cart.application.usecase.ModifyCartItemUseCase;
import com.book.core.cart.domain.Cart;
import com.book.core.cart.domain.CartItem;
import com.book.core.product.domain.Product;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;



@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class CartWriterConcurrencyIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    EntityManager entityManager;

    @Autowired
    CartItemRepositoryPort items;

    @Autowired
    CartRepositoryPort carts;

    @Autowired
    AddCartItemUseCase add;

    @Autowired
    ModifyCartItemUseCase modify;

    @Autowired
    DeleteCartItemUseCase delete;

    @Autowired
    DeleteCartItemsUseCase deleteMany;

    @Autowired
    DeleteCartItemsByProductsUseCase deleteByProducts;

    @Autowired
    ApplyCartChangesUseCase batch;

    @Test
    void 에이전트_기록_없이_일반_일괄변경을_실행한다() {
        createCart(920001L, 920002L);
        createProduct(920003L);
        final ApplyCartChangesCommand command =
            new ApplyCartChangesCommand(920002L, List.of(new CartItemChange(920003L, 0, 2, new BigDecimal("18000"))), null);

        assertThat(batch.execute(command).appliedAt()).isNotNull();
        assertThat(items.findActiveByCartId(920001L)).extracting("quantity").containsExactly(2);
    }

    @Test
    void 일괄변경은_호출자_트랜잭션과_함께_롤백한다() {
        createCart(921001L, 921002L);
        createProduct(921003L);
        final ApplyCartChangesCommand command =
            new ApplyCartChangesCommand(921002L, List.of(new CartItemChange(921003L, 0, 2, new BigDecimal("18000"))), null);

        assertThatThrownBy(() -> transaction().executeWithoutResult((final TransactionStatus status) -> {
            assertThat(batch.execute(command).appliedAt()).isNotNull();
            throw new IllegalStateException("test rollback");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(items.findActiveByCartId(921001L)).isEmpty();
    }

    @Test
    void 기한이_지난_일괄변경은_어떤_항목도_변경하지_않는다() {
        createCart(922001L, 922002L);
        createProduct(922003L);
        final ApplyCartChangesCommand command =
            new ApplyCartChangesCommand(922002L, List.of(new CartItemChange(922003L, 0, 2, new BigDecimal("18000"))), Instant.EPOCH);

        assertThat(batch.execute(command).expired()).isTrue();
        assertThat(items.findActiveByCartId(922001L)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"modify", "delete", "delete_many"})
    void 기존_트랜잭션의_오래된_항목을_사용해_삭제된_상품을_다시_변경하지_않는다(final String writer) {
        final long base = 71000L + writerOffset(writer);
        final long cartId = base + 1;
        final long itemId = base + 2;
        final long productId = base + 3;
        final long userId = base + 4;
        createCart(cartId, userId);
        createProduct(productId);
        createItem(itemId, cartId, productId, 2);

        try (final ExecutorService executor = Executors.newSingleThreadExecutor()) {
            assertThatThrownBy(() -> transaction().executeWithoutResult((final TransactionStatus status) -> {
                assertThat(jdbc.queryForObject("SELECT @@transaction_isolation", String.class)).isEqualTo("REPEATABLE-READ");
                assertThat(items.findActiveByCartId(cartId).getFirst().quantity()).isEqualTo(2);
                CompletableFuture.runAsync(() -> transaction().executeWithoutResult((final TransactionStatus other) -> {
                    jdbc.update("UPDATE cart_item SET quantity = 5, deleted_at = CURRENT_TIMESTAMP(6) WHERE id = ?", itemId);
                    jdbc.update("UPDATE carts SET version = version + 1 WHERE id = ?", cartId);
                }), executor).orTimeout(10, TimeUnit.SECONDS).join();

                write(writer, userId, itemId, productId);
            })).isInstanceOf(OptimisticLockingFailureException.class);
        }

        assertThat(jdbc.queryForObject("SELECT quantity FROM cart_item WHERE id = ?", Integer.class, itemId)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT deleted_at IS NOT NULL FROM cart_item WHERE id = ?", Boolean.class, itemId)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"add", "modify", "delete", "delete_many", "delete_by_products", "batch"})
    void 모든_변경은_자식만_바꿔도_부모_버전_경합을_커밋에서_검출한다(final String writer) {
        final long base = 100000L + writerOffset(writer);
        final long cartId = base + 1;
        final long itemId = base + 2;
        final long productId = base + 3;
        final long userId = base + 4;
        createCart(cartId, userId);
        createProduct(productId);
        createItem(itemId, cartId, productId, 2);
        try (final ExecutorService executor = Executors.newSingleThreadExecutor()) {
            assertThatThrownBy(() -> transaction().executeWithoutResult((final TransactionStatus status) -> {
                assertThat(carts.findByUserId(userId)).isPresent();
                // 조회는 경쟁 writer를 막지 않습니다. 늦게 끝나는 트랜잭션의 커밋이 실패합니다.
                CompletableFuture.runAsync(() -> write(writer, userId, itemId, productId), executor).orTimeout(10, TimeUnit.SECONDS).join();
                write(writer, userId, itemId, productId);
            })).isInstanceOf(OptimisticLockingFailureException.class);
        }
        assertThat(jdbc.queryForObject("SELECT version FROM carts WHERE id = ?", Long.class, cartId)).isPositive();
        assertThat(jdbc.queryForObject("SELECT deleted_at IS NOT NULL FROM cart_item WHERE id = ?", Boolean.class, itemId))
            .isEqualTo(writer.startsWith("delete"));
    }

    @Test
    void 담기는_이전_트랜잭션_조회_뒤_늘어난_항목을_포함해_30종_제한을_검증한다() {
        final long cartId = 200001L;
        final long userId = 200002L;
        final long targetProductId = 200003L;
        createCart(cartId, userId);
        createProduct(targetProductId);
        for (int index = 0; index < 29; index++) {
            createItem(200100L + index, cartId, 200200L + index, 1);
        }

        try (final ExecutorService executor = Executors.newSingleThreadExecutor()) {
            assertThatThrownBy(() -> transaction().executeWithoutResult((final TransactionStatus status) -> {
                assertThat(items.findActiveByCartId(cartId)).hasSize(29);
                CompletableFuture.runAsync(() -> transaction().executeWithoutResult((final TransactionStatus other) -> {
                    createItem(200500L, cartId, 200501L, 1);
                    jdbc.update("UPDATE carts SET version = version + 1 WHERE id = ?", cartId);
                }), executor).orTimeout(10, TimeUnit.SECONDS).join();

                add.execute(new AddCartItemCommand(userId, targetProductId, 1));
            })).isInstanceOf(OptimisticLockingFailureException.class);
        }

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cart_item WHERE cart_id = ? AND deleted_at IS NULL", Integer.class, cartId))
            .isEqualTo(30);
    }

    @Test
    void 결제_후_제거도_오래된_스냅샷의_버전_경합이면_전체_롤백한다() {
        final long cartId = 300001L;
        final long userId = 300002L;
        final long productId = 300003L;
        final long itemId = 300004L;
        createCart(cartId, userId);
        createProduct(productId);

        try (final ExecutorService executor = Executors.newSingleThreadExecutor()) {
            assertThatThrownBy(() -> transaction().executeWithoutResult((final TransactionStatus status) -> {
                assertThat(items.findActiveByCartId(cartId)).isEmpty();
                CompletableFuture.runAsync(() -> transaction().executeWithoutResult((final TransactionStatus other) -> {
                    createItem(itemId, cartId, productId, 2);
                    jdbc.update("UPDATE carts SET version = version + 1 WHERE id = ?", cartId);
                }), executor).orTimeout(10, TimeUnit.SECONDS).join();

                deleteByProducts.execute(userId, List.of(productId));
            })).isInstanceOf(OptimisticLockingFailureException.class);
        }

        assertThat(jdbc.queryForObject("SELECT deleted_at IS NOT NULL FROM cart_item WHERE id = ?", Boolean.class, itemId)).isFalse();
    }

    @Test
    void 항목_충돌은_다른_관리객체의_미커밋_변경까지_롤백한다() {
        final long cartId = 400001L;
        final long userId = 400002L;
        final long productId = 400003L;
        final long itemId = 400004L;
        final long otherProductId = 400005L;
        createCart(cartId, userId);
        createProduct(productId);
        createProduct(otherProductId);
        createItem(itemId, cartId, productId, 2);

        try (final ExecutorService executor = Executors.newSingleThreadExecutor()) {
            assertThatThrownBy(() -> transaction().executeWithoutResult((final TransactionStatus status) -> {
                final Cart cart = carts.findByUserId(userId).orElseThrow();
                final CartItem staleItem = items.findActiveByCartId(cartId).getFirst();
                assertThat(staleItem.quantity()).isEqualTo(2);
                final Product otherProduct = entityManager.find(Product.class, otherProductId);
                otherProduct.delete();
                CompletableFuture.runAsync(() -> transaction().executeWithoutResult((final TransactionStatus other) -> {
                    jdbc.update("UPDATE cart_item SET quantity = 5 WHERE id = ?", itemId);
                    jdbc.update("UPDATE carts SET version = version + 1 WHERE id = ?", cartId);
                }), executor).orTimeout(10, TimeUnit.SECONDS).join();

                assertThat(carts.findByUserIdForChange(userId)).isPresent();
                final CartItem currentItem = items.findAllByCartIdForChange(cartId).getFirst();
                assertThat(currentItem.quantity()).isEqualTo(2);
                assertThat(entityManager.contains(currentItem)).isTrue();
                assertThat(entityManager.contains(cart)).isTrue();
                assertThat(entityManager.contains(otherProduct)).isTrue();

                currentItem.applyQuantity(4);
                assertThat(items.findAllByCartIdForChange(cartId).getFirst().quantity()).isEqualTo(4);
            })).isInstanceOf(OptimisticLockingFailureException.class);
        }

        assertThat(jdbc.queryForObject("SELECT quantity FROM cart_item WHERE id = ?", Integer.class, itemId)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT deleted_at IS NOT NULL FROM products WHERE id = ?", Boolean.class, otherProductId))
            .isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void 같은_상품과_다른_상품의_동시_담기에서도_한_트랜잭션만_버전_경합을_통과한다(final boolean sameProduct) throws Exception {
        long base = 600000L;
        if (sameProduct) {
            base = 610000L;
        }
        final long userId = base;
        final long cartId = base + 1;
        final long firstProduct = base + 2;
        final long secondProduct = base + 3;
        createCart(cartId, userId);
        createProduct(firstProduct);
        createProduct(secondProduct);
        final CountDownLatch ready = new CountDownLatch(2);
        final CountDownLatch start = new CountDownLatch(1);
        final AtomicInteger success = new AtomicInteger();
        final AtomicInteger conflicts = new AtomicInteger();
        try (final ExecutorService executor = Executors.newFixedThreadPool(2)) {
            final List<Future<?>> running = new ArrayList<>();
            for (int index = 0; index < 2; index++) {
                long selected = secondProduct;
                if (sameProduct || index == 0) {
                    selected = firstProduct;
                }
                final long productId = selected;
                final int quantity = index + 1;
                running.add(executor.submit(() -> {
                    try {
                        transaction().executeWithoutResult((final TransactionStatus status) -> {
                            assertThat(carts.findByUserId(userId)).isPresent();
                            ready.countDown();
                            await(start);
                            add.execute(new AddCartItemCommand(userId, productId, quantity));
                        });
                        success.incrementAndGet();
                    } catch (final OptimisticLockingFailureException conflict) {
                        conflicts.incrementAndGet();
                    }
                }));
            }
            try {
                await(ready);
            } finally {
                start.countDown();
            }
            for (final Future<?> task : running) {
                task.get(10, TimeUnit.SECONDS);
            }
        }
        assertThat(success.get()).isEqualTo(1);
        assertThat(conflicts.get()).isEqualTo(1);
        assertThat(items.findActiveByCartId(cartId)).hasSize(1);
    }

    private void write(final String writer, final long userId, final long itemId, final long productId) {
        switch (writer) {
            case "batch" -> batch
                .execute(new ApplyCartChangesCommand(userId, List.of(new CartItemChange(productId, 2, 4, new BigDecimal("18000"))), null));
            case "add" -> add.execute(new AddCartItemCommand(userId, productId, 4));
            case "modify" -> modify.execute(new ModifyCartItemCommand(userId, itemId, 4));
            case "delete" -> delete.execute(new DeleteCartItemCommand(userId, itemId));
            case "delete_many" -> deleteMany.execute(new DeleteCartItemsCommand(userId, List.of(itemId)));
            case "delete_by_products" -> deleteByProducts.execute(userId, List.of(productId));
            default -> throw new IllegalArgumentException(writer);
        }
    }

    private static int writerOffset(final String writer) {
        return switch (writer) {
            case "add" -> 0;
            case "modify" -> 1000;
            case "delete" -> 2000;
            case "delete_many" -> 3000;
            case "delete_by_products" -> 4000;
            case "batch" -> 5000;
            default -> throw new IllegalArgumentException(writer);
        };
    }

    private static void await(final CountDownLatch latch) {
        try {
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private TransactionTemplate transaction() {
        return new TransactionTemplate(transactionManager);
    }

    private void createCart(final long cartId, final long userId) {
        jdbc.update("INSERT INTO carts (id, user_id) VALUES (?, ?)", cartId, userId);
    }

    private void createProduct(final long productId) {
        jdbc.update("""
                INSERT INTO books (id, title, author, publisher, category, published_at)
                VALUES (?, '도서', '작가', '출판사', '소설', '2026-01-01')
                """, productId);
        jdbc.update("""
                INSERT INTO products (id, book_id, name, sale_price, discounted_price, cost_price, stock_quantity)
                VALUES (?, ?, '상품', 20000, 18000, 10000, 500)
                """, productId, productId);
    }

    private void createItem(final long itemId, final long cartId, final long productId, final int quantity) {
        jdbc.update("INSERT INTO cart_item (id, cart_id, product_id, quantity) VALUES (?, ?, ?, ?)", itemId, cartId, productId, quantity);
    }
}
