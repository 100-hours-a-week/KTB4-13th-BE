package com.book.core.product.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.product.application.port.ProductRepositoryPort;
import java.sql.Timestamp;
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
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class ProductRepositoryIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    ProductRepositoryPort productRepository;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 활성_상품과_활성_도서가_연결된_상세만_조회한다() {
        insertBook(1001L, "활성 도서", null);
        insertBook(1002L, "삭제 도서", Timestamp.valueOf("2026-01-01 00:00:00"));
        insertProduct(2001L, 1001L, "활성 상품", null);
        insertProduct(2002L, 1001L, "삭제 상품", Timestamp.valueOf("2026-01-01 00:00:00"));
        insertProduct(2003L, 1002L, "도서가 삭제된 상품", null);

        final var found = productRepository.findActiveById(2001L).orElseThrow();

        assertThat(found.name()).isEqualTo("활성 상품");
        assertThat(found.book().title()).isEqualTo("활성 도서");
        assertThat(productRepository.findActiveById(2002L)).isEmpty();
        assertThat(productRepository.findActiveById(2003L)).isEmpty();
    }

    @Test
    void 카테고리와_활성_조건을_적용하고_생성일과_ID_기준_커서로_상품을_조회한다() {
        insertCategory(3001L, "소설", null);
        insertCategory(3002L, "삭제 카테고리", Timestamp.valueOf("2026-01-01 00:00:00"));
        insertBook(1003L, "목록 도서 1", null);
        insertBook(1004L, "목록 도서 2", null);
        insertBook(1005L, "목록 도서 3", null);
        insertBook(1006L, "삭제 도서", Timestamp.valueOf("2026-01-01 00:00:00"));
        insertBook(1007L, "비활성 연결 도서", null);
        insertProduct(2101L, 1003L, "상품 1", null, Timestamp.valueOf("2026-01-03 00:00:00"));
        insertProduct(2102L, 1004L, "상품 2", null, Timestamp.valueOf("2026-01-01 00:00:00"));
        insertProduct(2103L, 1005L, "상품 3", null, Timestamp.valueOf("2026-01-02 00:00:00"));
        insertProduct(2104L, 1006L, "삭제 도서 상품", null, Timestamp.valueOf("2026-01-01 00:00:00"));
        insertProduct(2105L, 1007L, "비활성 연결 상품", null, Timestamp.valueOf("2026-01-01 00:00:00"));
        insertProductCategory(3101L, 3001L, 2101L, null);
        insertProductCategory(3102L, 3001L, 2102L, null);
        insertProductCategory(3103L, 3001L, 2103L, null);
        insertProductCategory(3104L, 3001L, 2104L, null);
        insertProductCategory(3105L, 3002L, 2101L, null);
        insertProductCategory(3106L, 3001L, 2105L, Timestamp.valueOf("2026-01-01 00:00:00"));

        final var firstPage = productRepository.findActiveProducts(3001L, null, 1);
        final var secondPage = productRepository.findActiveProducts(3001L, 2101L, 2);

        assertThat(firstPage).extracting(product -> product.id()).containsExactly(2101L);
        assertThat(secondPage).extracting(product -> product.id()).containsExactly(2103L, 2102L);
    }

    private void insertBook(final long id, final String title, final Timestamp deletedAt) {
        jdbc.update("""
                INSERT INTO books (
                    id, title, author, publisher, category, published_at, deleted_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, id, title, "작가", "출판사", "소설", "2026-01-01", deletedAt);
    }

    private void insertProduct(final long id, final long bookId, final String name, final Timestamp deletedAt) {
        insertProduct(id, bookId, name, deletedAt, Timestamp.valueOf("2026-01-01 00:00:00"));
    }

    private void insertProduct(
            final long id, final long bookId, final String name, final Timestamp deletedAt, final Timestamp createdAt) {
        jdbc.update("""
                INSERT INTO products (
                    id, book_id, name, sale_price, discounted_price, cost_price, stock_quantity,
                    deleted_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, bookId, name, 20000.00, 18000.00, 12000.00, 10, deletedAt, createdAt, createdAt);
    }

    private void insertCategory(final long id, final String name, final Timestamp deletedAt) {
        jdbc.update("""
                INSERT INTO book_category (id, name, path, deleted_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, id, name, "도서/" + name, deletedAt);
    }

    private void insertProductCategory(
            final long id, final long categoryId, final long productId, final Timestamp deletedAt) {
        jdbc.update("""
                INSERT INTO product_category (id, category_id, product_id, deleted_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, id, categoryId, productId, deletedAt);
    }
}
