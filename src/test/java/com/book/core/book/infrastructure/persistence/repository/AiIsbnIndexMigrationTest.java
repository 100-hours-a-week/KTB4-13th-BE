package com.book.core.book.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@Tag("integration")
@Tag("mysql-migration")
class AiIsbnIndexMigrationTest {
    @Container
    final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void 기존_카탈로그를_보존하고_ISBN_조회_인덱스를_중복_생성하지_않는다(final boolean alreadyIndexed) {
        final var dataSource = new DriverManagerDataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
        Flyway.configure().dataSource(dataSource).target("72").load().migrate();
        final JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        if (alreadyIndexed) {
            jdbc.execute("CREATE UNIQUE INDEX catalog_isbn_unique ON books (isbn13)");
        }
        jdbc.update("""
            INSERT INTO books (title, author, publisher, category, published_at)
            VALUES ('ISBN 확인 중', '저자', '출판사', '소설', '2026-01-01')
            """);

        Flyway.configure().dataSource(dataSource).load().migrate();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM books WHERE title = 'ISBN 확인 중' AND isbn13 IS NULL", Long.class))
            .isEqualTo(1L);
        final List<String> indexes = jdbc.queryForList("""
            SELECT index_name FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = 'books' AND column_name = 'isbn13' AND seq_in_index = 1
            """, String.class);
        if (alreadyIndexed) {
            assertThat(indexes).containsExactly("catalog_isbn_unique");
        } else {
            assertThat(indexes).containsExactly("idx_books_isbn13");
        }
    }
}
