package com.book.core.point.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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
class PointBalanceVersionMigrationTest {
    @Container
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Test
    void 기존_잔액과_신규_행의_버전을_0으로_초기화한다() {
        final DriverManagerDataSource dataSource =
            new DriverManagerDataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
        Flyway.configure().dataSource(dataSource).target("24").load().migrate();
        final JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO point_balance (user_id, balance, created_at, updated_at) VALUES (1, 1000, NOW(), NOW())");

        Flyway.configure().dataSource(dataSource).load().migrate();

        assertThat(jdbc.queryForObject("SELECT balance FROM point_balance WHERE user_id = 1", BigDecimal.class))
            .isEqualByComparingTo("1000");
        assertThat(jdbc.queryForObject("SELECT version FROM point_balance WHERE user_id = 1", Long.class)).isZero();
        jdbc.update("INSERT INTO point_balance (user_id, balance, created_at, updated_at) VALUES (2, 500, NOW(), NOW())");
        assertThat(jdbc.queryForObject("SELECT version FROM point_balance WHERE user_id = 2", Long.class)).isZero();
        assertThat(jdbc.queryForObject("""
            SELECT IS_NULLABLE FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'point_balance' AND COLUMN_NAME = 'version'
            """, String.class)).isEqualTo("NO");
    }
}
