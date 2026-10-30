package com.book.core.point.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@Tag("integration")
@Tag("mysql-migration")
class PointReservationMigrationTest {
    @Container
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Test
    void 기존_음수_잔액을_확인_대상에_원본_그대로_남기고_미회수로_분류하지_않는다() {
        final DriverManagerDataSource dataSource =
            new DriverManagerDataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
        Flyway.configure().dataSource(dataSource).target("69").load().migrate();
        final JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO point_balance (user_id, balance, version, created_at, updated_at) VALUES (70001, -500, 0, NOW(), NOW())");
        jdbc.update("INSERT INTO point_balance (user_id, balance, version, created_at, updated_at) VALUES (70002, 1000, 0, NOW(), NOW())");
        jdbc.update("INSERT INTO point_history (user_id, type, reference_id, amount, balance_after, created_at, updated_at) "
            + "VALUES (70002, 1, 99, 1000, 1000, NOW(), NOW())");
        Flyway.configure().dataSource(dataSource).load().migrate();

        assertThat(jdbc.queryForObject("SELECT balance FROM point_balance WHERE user_id = 70001", BigDecimal.class))
            .isEqualByComparingTo("0");
        assertThat(
            jdbc.queryForObject("SELECT original_balance FROM point_unclassified_negative_balance WHERE user_id = 70001", BigDecimal.class))
            .isEqualByComparingTo("-500");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM point_recovery WHERE user_id = 70001", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT balance FROM point_balance WHERE user_id = 70002", BigDecimal.class))
            .isEqualByComparingTo("1000");
        assertThat(jdbc.queryForObject("SELECT amount FROM point_history WHERE user_id = 70002", BigDecimal.class))
            .isEqualByComparingTo("1000");
        assertThat(jdbc.queryForObject("SELECT balance_after FROM point_history WHERE user_id = 70002", BigDecimal.class))
            .isEqualByComparingTo("1000");
        for (final String[] column : new String[][] {{"point_balance", "balance"}, {"point_history", "amount"},
            {"point_history", "balance_after"}}) {
            assertThat(jdbc.queryForObject("SELECT NUMERIC_PRECISION FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?", Integer.class, column[0], column[1]))
                .isEqualTo(38);
            assertThat(jdbc.queryForObject("SELECT NUMERIC_SCALE FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?", Integer.class, column[0], column[1]))
                .isEqualTo(2);
        }
        jdbc.update("INSERT INTO point_recovery "
            + "(user_id, source_type, source_reference_id, original_amount, remaining_amount, created_at, updated_at) "
            + "VALUES (70002, 'PAYMENT', 55, 300, 300, NOW(), NOW())");
        jdbc.update("INSERT INTO point_recovery "
            + "(user_id, source_type, source_reference_id, original_amount, remaining_amount, created_at, updated_at) "
            + "VALUES (70002, 'REVIEW', 55, 500, 500, NOW(), NOW())");
        assertThat(jdbc.queryForList("SELECT source_type FROM point_recovery WHERE user_id = 70002 ORDER BY id", String.class))
            .containsExactly("PAYMENT", "REVIEW");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO point_recovery "
            + "(user_id, source_type, source_reference_id, original_amount, remaining_amount, created_at, updated_at) "
            + "VALUES (70002, 'REVIEW', 55, 500, 500, NOW(), NOW())")).isInstanceOf(DataIntegrityViolationException.class);
    }
}
