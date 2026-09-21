package com.book.core.product.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.product.application.port.ProductRepositoryPort;
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

    private void insertBook(final long id, final String title, final String status) {
        jdbc.update("""
                INSERT INTO books (
                    id, title, author, publisher, category, published_at, status, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, id, title, "작가", "출판사", "소설", "2026-01-01", status);
    }

    private void insertProduct(final long id, final long bookId, final String name, final String status) {
        jdbc.update("""
                INSERT INTO products (
                    id, book_id, name, sale_price, discounted_price, cost_price, stock_quantity,
                    status, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, id, bookId, name, 20000.00, 18000.00, 12000.00, 10, status);
    }
}
