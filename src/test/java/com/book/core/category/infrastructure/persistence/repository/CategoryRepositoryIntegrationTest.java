package com.book.core.category.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.category.application.port.CategoryRepositoryPort;
import com.book.core.category.domain.Category;
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
class CategoryRepositoryIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    CategoryRepositoryPort categoryRepository;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 삭제되지_않은_카테고리만_조회한다() {
        jdbc.update(
                "INSERT INTO book_category (name, path, created_at, updated_at) VALUES (?, ?, ?, ?)",
                "소설",
                "도서/소설",
                "2026-01-01 00:00:00",
                "2026-01-01 00:00:00");
        jdbc.update(
                "INSERT INTO book_category (name, path, deleted_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
                "삭제 카테고리",
                "도서/삭제",
                "2026-01-02 00:00:00",
                "2026-01-01 00:00:00",
                "2026-01-02 00:00:00");

        assertThat(categoryRepository.findActiveCategories())
                .extracting(Category::name, Category::path)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("소설", "도서/소설"));
    }
}
