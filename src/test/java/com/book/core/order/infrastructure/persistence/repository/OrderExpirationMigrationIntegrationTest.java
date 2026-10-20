package com.book.core.order.infrastructure.persistence.repository;

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
class OrderExpirationMigrationIntegrationTest {
    @Container
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Test
    void 예약_이전의_미결제_주문은_PG_승인이_진행_중일_때만_유지하고_나머지는_만료한다() {
        final DriverManagerDataSource dataSource =
            new DriverManagerDataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
        Flyway.configure().dataSource(dataSource).target("58").load().migrate();
        final JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        for (final String orderKey : new String[] {"unpaid", "pending", "rejected"}) {
            jdbc.update("INSERT INTO orders (user_id, `key`, total_price, status) VALUES (1, ?, 100, 'CREATED')", orderKey);
        }
        jdbc.update("INSERT INTO orders (user_id, `key`, total_price, status) VALUES (1, 'paid', 100, 'PAID')");
        insertPgOperation(jdbc, "pending", 0);
        insertPgOperation(jdbc, "rejected", 1);

        Flyway.configure().dataSource(dataSource).load().migrate();

        assertThat(jdbc.queryForList("SELECT CONCAT(`key`, ':', status) FROM orders ORDER BY id", String.class))
            .containsExactly("unpaid:EXPIRED", "pending:CREATED", "rejected:EXPIRED", "paid:PAID");
    }

    private static void insertPgOperation(final JdbcTemplate jdbc, final String orderKey, final int rejected) {
        jdbc.update("""
            INSERT INTO pg_operation (order_key, request_hash, input_hash, pg_idempotency_key, type, requested_at, completed, rejected,
                created_at, updated_at)
            VALUES (?, ?, 'input', UUID(), 'APPROVE', NOW(6), 0, ?, NOW(6), NOW(6))
            """, orderKey, "request-" + orderKey, rejected);
    }
}
