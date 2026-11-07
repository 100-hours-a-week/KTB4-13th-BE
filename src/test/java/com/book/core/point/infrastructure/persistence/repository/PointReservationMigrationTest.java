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
    void 예약_마이그레이션은_기존_잔액을_변경하지_않고_예약_잔액과_시도_유일성을_추가한다() {
        final DriverManagerDataSource dataSource =
            new DriverManagerDataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
        Flyway.configure().dataSource(dataSource).target("69").load().migrate();
        final JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO point_balance (user_id, balance, version, created_at, updated_at) VALUES (70001, -500, 0, NOW(), NOW())");
        jdbc.update("INSERT INTO point_balance (user_id, balance, version, created_at, updated_at) VALUES (70002, 1000, 0, NOW(), NOW())");

        Flyway.configure().dataSource(dataSource).load().migrate();

        assertThat(jdbc.queryForObject("SELECT balance FROM point_balance WHERE user_id = 70001", BigDecimal.class))
            .isEqualByComparingTo("-500");
        assertThat(jdbc.queryForObject("SELECT balance FROM point_balance WHERE user_id = 70002", BigDecimal.class))
            .isEqualByComparingTo("1000");
        assertThat(jdbc.queryForObject("SELECT reserved_amount FROM point_balance WHERE user_id = 70002", BigDecimal.class))
            .isEqualByComparingTo("0");

        jdbc.update("INSERT INTO point_reservation (created_at, updated_at, user_id, attempt_id, payment_id, amount, reservation_state) "
            + "VALUES (NOW(), NOW(), 70002, 80001, 90001, 300, 'RESERVED')");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM point_reservation WHERE attempt_id = 80001", Long.class)).isEqualTo(1L);
        assertThatThrownBy(() -> jdbc
            .update("INSERT INTO point_reservation (created_at, updated_at, user_id, attempt_id, payment_id, amount, reservation_state) "
                + "VALUES (NOW(), NOW(), 70002, 80001, 90001, 300, 'RESERVED')"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
