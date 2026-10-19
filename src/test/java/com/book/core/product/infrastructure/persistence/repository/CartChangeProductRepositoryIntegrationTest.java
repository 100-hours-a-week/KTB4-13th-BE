package com.book.core.product.infrastructure.persistence.repository;

import org.springframework.transaction.TransactionStatus;
import org.springframework.dao.OptimisticLockingFailureException;
import java.util.concurrent.ExecutorService;
import java.util.Map;
import com.book.core.product.application.result.CartChangeProductResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.core.product.application.usecase.GetProductsForCartChangeUseCase;
import com.book.core.product.domain.Product;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles({"test", "mysql-test"})
@Testcontainers
@Tag("integration")
@Tag("mysql-migration")
@Timeout(30)
class CartChangeProductRepositoryIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    GetProductsForCartChangeUseCase useCase;
    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    EntityManager entityManager;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 호출자의_트랜잭션이_없으면_조회하지_않는다() {
        assertThatThrownBy(() -> useCase.execute(List.of(950001L))).isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void 없는_상품은_제외하고_삭제_상품과_도서는_비활성으로_반환한다() {
        insertProduct(950011L);
        insertProduct(950012L);
        insertProduct(950013L);
        jdbc.update("UPDATE products SET stock_quantity = 0 WHERE id = ?", 950011L);
        jdbc.update("UPDATE products SET deleted_at = NOW() WHERE id = ?", 950012L);
        jdbc.update("UPDATE books SET deleted_at = NOW(), version = version + 1 WHERE id = ?", 950013L);

        new TransactionTemplate(transactionManager).executeWithoutResult((final TransactionStatus status) -> {
            final Map<Long, CartChangeProductResult> result = useCase.execute(List.of(950013L, 950011L, 950012L, 999999L, 950011L));

            assertThat(result).containsOnlyKeys(950011L, 950012L, 950013L);
            assertThat(result.get(950011L).active()).isTrue();
            assertThat(result.get(950011L).stockQuantity()).isZero();
            assertThat(result.get(950012L).active()).isFalse();
            assertThat(result.get(950013L).active()).isFalse();
        });
    }

    @Test
    void 오래된_영속객체는_상품과_도서_버전_경합으로_커밋을_거절한다() {
        insertProduct(950021L);
        final TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        final TransactionTemplate externalChange = new TransactionTemplate(transactionManager);
        externalChange.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> transaction.executeWithoutResult((final TransactionStatus status) -> {
            final Product previous = entityManager.find(Product.class, 950021L);
            assertThat(previous.discountedPrice()).isEqualByComparingTo("18000.00");
            assertThat(previous.book().isActive()).isTrue();

            externalChange.executeWithoutResult((final TransactionStatus other) -> {
                jdbc.update("UPDATE products SET discounted_price = 19500, stock_quantity = 7, version = version + 1 WHERE id = ?",
                    950021L);
                jdbc.update("UPDATE books SET deleted_at = NOW(), version = version + 1 WHERE id = ?", 950021L);
            });
            assertThat(entityManager.find(Product.class, 950021L)).isSameAs(previous);
            assertThat(previous.discountedPrice()).isEqualByComparingTo("18000.00");

            final CartChangeProductResult current = useCase.execute(List.of(950021L)).get(950021L);

            assertThat(current.unitPrice()).isEqualByComparingTo("18000.00");
            assertThat(current.stockQuantity()).isEqualTo(5);
            assertThat(current.active()).isTrue();
        })).isInstanceOf(OptimisticLockingFailureException.class);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"price", "stock", "product_delete", "book_delete"})
    void 조회_뒤_상품과_도서_변경은_대기하지_않고_완료되며_기존_트랜잭션은_커밋_실패한다(final String change) {
        final long id = 960000L + change.hashCode() % 100000;
        insertProduct(id);
        try (final ExecutorService executor = Executors.newSingleThreadExecutor()) {
            assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult((final TransactionStatus status) -> {
                useCase.execute(List.of(id));
                final Future<Integer> changed = executor.submit(() -> {
                    final String sql = switch (change) {
                        case "price" -> "UPDATE products SET discounted_price = 19500, version = version + 1 WHERE id = ?";
                        case "stock" -> "UPDATE products SET stock_quantity = 9, version = version + 1 WHERE id = ?";
                        case "product_delete" -> "UPDATE products SET deleted_at = NOW(), version = version + 1 WHERE id = ?";
                        case "book_delete" -> "UPDATE books SET deleted_at = NOW(), version = version + 1 WHERE id = ?";
                        default -> throw new IllegalArgumentException(change);
                    };
                    return jdbc.update(sql, id);
                });
                try {
                    assertThat(changed.get(5, TimeUnit.SECONDS)).isEqualTo(1);
                } catch (final Exception exception) {
                    throw new AssertionError(exception);
                }
            })).isInstanceOf(OptimisticLockingFailureException.class);
        }
    }

    private void insertProduct(final long id) {
        jdbc.update("""
                INSERT INTO books (id, title, author, publisher, category, published_at, created_at, updated_at)
                VALUES (?, '도서', '작가', '출판사', '소설', '2026-01-01', NOW(), NOW())
                """, id);
        jdbc.update("""
                INSERT INTO products (id, book_id, name, sale_price, discounted_price, cost_price, stock_quantity,
                    created_at, updated_at)
                VALUES (?, ?, '상품', 20000, 18000, 12000, 5, NOW(), NOW())
                """, id, id);
    }
}
