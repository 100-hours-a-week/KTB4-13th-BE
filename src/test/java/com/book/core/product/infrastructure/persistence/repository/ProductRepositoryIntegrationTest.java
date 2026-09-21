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
        insertBook(1001L, "활성 도서", "ACTIVE");
        insertBook(1002L, "삭제 도서", "DELETED");
        insertProduct(2001L, 1001L, "활성 상품", "ACTIVE");
        insertProduct(2002L, 1001L, "삭제 상품", "DELETED");
        insertProduct(2003L, 1002L, "도서가 삭제된 상품", "ACTIVE");

        final var found = productRepository.findActiveById(2001L).orElseThrow();

        assertThat(found.name()).isEqualTo("활성 상품");
        assertThat(found.book().title()).isEqualTo("활성 도서");
        assertThat(productRepository.findActiveById(2002L)).isEmpty();
        assertThat(productRepository.findActiveById(2003L)).isEmpty();
    }

    @Test
    void 카테고리와_활성_조건을_적용하고_ID_내림차순_커서로_상품을_조회한다() {
        insertCategory(3001L, "소설", "ACTIVE");
        insertCategory(3002L, "삭제 카테고리", "DELETED");
        insertBook(1003L, "목록 도서 1", "ACTIVE");
        insertBook(1004L, "목록 도서 2", "ACTIVE");
        insertBook(1005L, "목록 도서 3", "ACTIVE");
        insertBook(1006L, "삭제 도서", "DELETED");
        insertBook(1007L, "비활성 연결 도서", "ACTIVE");
        insertProduct(2101L, 1003L, "상품 1", "ACTIVE", Timestamp.valueOf("2026-01-01 00:00:00"));
        insertProduct(2102L, 1004L, "상품 2", "ACTIVE", Timestamp.valueOf("2026-01-01 00:00:00"));
        insertProduct(2103L, 1005L, "상품 3", "ACTIVE", Timestamp.valueOf("2026-01-01 00:00:00"));
        insertProduct(2104L, 1006L, "삭제 도서 상품", "ACTIVE", Timestamp.valueOf("2026-01-01 00:00:00"));
        insertProduct(2105L, 1007L, "비활성 연결 상품", "ACTIVE", Timestamp.valueOf("2026-01-01 00:00:00"));
        insertProductCategory(3101L, 3001L, 2101L, "ACTIVE");
        insertProductCategory(3102L, 3001L, 2102L, "ACTIVE");
        insertProductCategory(3103L, 3001L, 2103L, "ACTIVE");
        insertProductCategory(3104L, 3001L, 2104L, "ACTIVE");
        insertProductCategory(3105L, 3002L, 2101L, "ACTIVE");
        insertProductCategory(3106L, 3001L, 2105L, "DELETED");

        final var firstPage = productRepository.findActiveProducts(3001L, null, 3);
        final var secondPage = productRepository.findActiveProducts(3001L, 2102L, 3);

        assertThat(firstPage).extracting(product -> product.id()).containsExactly(2103L, 2102L, 2101L);
        assertThat(secondPage).extracting(product -> product.id()).containsExactly(2101L);
    }

    private void insertBook(final long id, final String title, final String status) {
        jdbc.update("""
                INSERT INTO books (
                    id, title, author, publisher, category, published_at, status, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, id, title, "작가", "출판사", "소설", "2026-01-01", status);
    }

    private void insertProduct(final long id, final long bookId, final String name, final String status) {
        insertProduct(id, bookId, name, status, Timestamp.valueOf("2026-01-01 00:00:00"));
    }

    private void insertProduct(
            final long id, final long bookId, final String name, final String status, final Timestamp createdAt) {
        jdbc.update("""
                INSERT INTO products (
                    id, book_id, name, sale_price, discounted_price, cost_price, stock_quantity,
                    status, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, bookId, name, 20000.00, 18000.00, 12000.00, 10, status, createdAt, createdAt);
    }

    private void insertCategory(final long id, final String name, final String status) {
        jdbc.update("""
                INSERT INTO book_category (id, name, path, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, id, name, "도서/" + name, status);
    }

    private void insertProductCategory(
            final long id, final long categoryId, final long productId, final String status) {
        jdbc.update("""
                INSERT INTO product_category (id, category_id, product_id, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, id, categoryId, productId, status);
    }
}
