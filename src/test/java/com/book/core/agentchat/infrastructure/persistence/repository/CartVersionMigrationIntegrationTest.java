package com.book.core.agentchat.infrastructure.persistence.repository;

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
class CartVersionMigrationIntegrationTest {
    @Container
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Test
    void main의_V49_데이터를_보존하며_알림과_쇼핑_migration을_순서대로_적용한다() {
        final DriverManagerDataSource dataSource =
            new DriverManagerDataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
        Flyway.configure().dataSource(dataSource).target("49").load().migrate();
        final JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO carts (user_id) VALUES (7)");
        jdbc.update("""
            INSERT INTO books (title, author, publisher, category, published_at)
            VALUES ('migration book', 'author', 'publisher', 'novel', '2026-01-01')
            """);
        jdbc.update("""
            INSERT INTO products (book_id, name, sale_price, discounted_price, cost_price, stock_quantity)
            VALUES (1, 'migration product', 12000, 10000, 8000, 5)
            """);
        jdbc.update("INSERT INTO cart_item (cart_id, product_id, quantity) VALUES (1, 1, 3)");

        Flyway.configure().dataSource(dataSource).load().migrate();

        assertThat(jdbc.queryForObject("SELECT quantity FROM cart_item WHERE cart_id = 1", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT version FROM carts WHERE id = 1", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT version FROM products WHERE id = 1", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT version FROM books WHERE id = 1", Long.class)).isZero();
        jdbc.update("INSERT INTO carts (user_id) VALUES (8)");
        assertThat(jdbc.queryForObject("SELECT version FROM carts WHERE user_id = 8", Long.class)).isZero();
        assertThat(jdbc.queryForList("""
            SELECT version FROM flyway_schema_history
            WHERE version IN ('50', '51', '52', '53', '54') AND success = 1 ORDER BY installed_rank
            """, String.class)).containsExactly("50", "51", "52", "53", "54");
    }
}
