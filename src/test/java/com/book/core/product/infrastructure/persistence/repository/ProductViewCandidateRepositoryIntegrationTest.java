package com.book.core.product.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.product.application.usecase.FindLowStockNotificationCandidatesUseCase;
import com.book.core.product.domain.ProductRestock;
import com.book.core.product.domain.ProductView;
import java.time.LocalDateTime;
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
class ProductViewCandidateRepositoryIntegrationTest {
    private static final AtomicLong NEXT_ID = new AtomicLong(4_600_000L);
    private static final LocalDateTime REFERENCE_TIME = LocalDateTime.of(2026, 10, 26, 12, 0);

    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    FindLowStockNotificationCandidatesUseCase useCase;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ProductViewJpaRepository productViewJpaRepository;

    @Autowired
    ProductRestockJpaRepository productRestockJpaRepository;

    @Test
    void 입고가_없으면_최근_7일_내_두_번_이상_조회한_사용자만_중복_없이_반환한다() {
        final long productId = insertProduct();
        final long otherProductId = insertProduct();
        insertView(101L, productId, REFERENCE_TIME.minusDays(7));
        insertView(101L, productId, REFERENCE_TIME);
        insertView(101L, productId, REFERENCE_TIME.minusHours(1));
        insertView(102L, productId, REFERENCE_TIME.minusDays(7).minusNanos(1000));
        insertView(102L, productId, REFERENCE_TIME.minusHours(1));
        insertView(103L, productId, REFERENCE_TIME.plusNanos(1000));
        insertView(103L, productId, REFERENCE_TIME.minusHours(1));
        insertView(104L, productId, REFERENCE_TIME.minusHours(1));
        insertView(104L, otherProductId, REFERENCE_TIME.minusMinutes(1));

        assertThat(useCase.execute(productId, REFERENCE_TIME)).containsExactly(101L);
    }

    @Test
    void 마지막_완료_입고_이후_조회만_집계한다() {
        final long productId = insertProduct();
        insertRestock(productId, "COMPLETED", REFERENCE_TIME.minusDays(5), null);
        insertRestock(productId, "COMPLETED", REFERENCE_TIME.minusDays(2), null);
        insertView(201L, productId, REFERENCE_TIME.minusDays(3));
        insertView(201L, productId, REFERENCE_TIME.minusDays(1));
        insertView(202L, productId, REFERENCE_TIME.minusDays(2));
        insertView(202L, productId, REFERENCE_TIME.minusDays(1));
        insertView(203L, productId, REFERENCE_TIME.minusDays(1));
        insertView(203L, productId, REFERENCE_TIME);

        assertThat(useCase.execute(productId, REFERENCE_TIME)).containsExactly(203L);
    }

    @Test
    void 대기_삭제_미래_입고는_마지막_완료_입고에서_제외한다() {
        final long productId = insertProduct();
        insertRestock(productId, "COMPLETED", REFERENCE_TIME.minusDays(5), null);
        insertRestock(productId, "PENDING", null, null);
        insertRestock(productId, "COMPLETED", REFERENCE_TIME.minusDays(2), REFERENCE_TIME.minusDays(1));
        insertRestock(productId, "COMPLETED", REFERENCE_TIME.plusDays(1), null);
        insertView(301L, productId, REFERENCE_TIME.minusDays(4));
        insertView(301L, productId, REFERENCE_TIME.minusDays(3));
        insertView(302L, productId, REFERENCE_TIME.minusDays(6));
        insertView(302L, productId, REFERENCE_TIME.minusDays(4));

        assertThat(useCase.execute(productId, REFERENCE_TIME)).containsExactly(301L);
    }

    private long insertProduct() {
        final long productId = NEXT_ID.incrementAndGet();
        jdbc.update("""
            INSERT INTO books (id, title, author, publisher, category, published_at)
            VALUES (?, '책', '작가', '출판사', '소설', '2026-01-01')
            """, productId);
        jdbc.update("""
            INSERT INTO products (id, book_id, name, sale_price, discounted_price, cost_price)
            VALUES (?, ?, '상품', 20000, 18000, 10000)
            """, productId, productId);
        return productId;
    }

    private void insertView(final long userId, final long productId, final LocalDateTime viewedAt) {
        productViewJpaRepository.saveAndFlush(new ProductView(userId, productId, viewedAt));
    }

    private void insertRestock(final long productId, final String status, final LocalDateTime receivedAt, final LocalDateTime deletedAt) {
        final ProductRestock restock = new ProductRestock(productId, 10);
        if ("COMPLETED".equals(status)) {
            restock.receive(receivedAt);
        }
        final ProductRestock saved = productRestockJpaRepository.saveAndFlush(restock);
        if (deletedAt != null) {
            jdbc.update("UPDATE product_restocks SET deleted_at = ? WHERE id = ?", deletedAt, saved.id());
        }
    }
}
