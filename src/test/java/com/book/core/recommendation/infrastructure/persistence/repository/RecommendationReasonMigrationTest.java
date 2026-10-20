package com.book.core.recommendation.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@Tag("integration")
@Tag("mysql-migration")
class RecommendationReasonMigrationTest {
    @Container
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Test
    void 기존_긴_이유를_보존하며_null_이유의_저장을_허용한다() {
        final DriverManagerDataSource source = new DriverManagerDataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
        Flyway.configure().dataSource(source).target("56").load().migrate();
        final JdbcTemplate jdbc = new JdbcTemplate(source);
        jdbc.update("INSERT INTO users (id, nickname) VALUES (1, 'migration')");
        jdbc.update("""
            INSERT INTO books (id, title, author, publisher, category, published_at)
            VALUES (1, '책', '작가', '출판사', '소설', '2026-01-01')
            """);
        jdbc.update("INSERT INTO recommendation_cards (user_id, book_id, reason_long) VALUES (1, 1, '기존 긴 이유')");

        Flyway.configure().dataSource(source).load().migrate();
        jdbc.update("INSERT INTO recommendation_cards (user_id, book_id, reason_long) VALUES (1, 1, NULL)");

        assertThat(jdbc.queryForObject("SELECT reason_long FROM recommendation_cards WHERE id = 1", String.class)).isEqualTo("기존 긴 이유");
        assertThat(jdbc.queryForObject("SELECT reason_long FROM recommendation_cards WHERE id = 2", String.class)).isNull();
        assertThat(jdbc.queryForObject("""
            SELECT IS_NULLABLE FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'recommendation_cards' AND column_name = 'reason_long'
            """, String.class)).isEqualTo("YES");
    }
}
