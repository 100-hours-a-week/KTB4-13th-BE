package com.book.core.coupon.infrastructure.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.coupon.application.command.AcceptedCouponIssue;
import com.book.core.coupon.application.port.CouponIssueQueuePort.Reservation;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles({"test", "mysql-test"})
@Testcontainers
@Tag("integration")
@Transactional
class CouponIssueRequestRepositoryIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    JdbcClient jdbc;

    @Test
    void 예약한_요청을_발행하고_완료한_요청은_다시_처리하지_않는다() {
        final long couponId = 559001;
        jdbc.sql("""
            INSERT INTO coupon (id, name, type, discount, max_use_count, min_order_amount,
                starts_at, expired_at, total_quantity, issued_quantity)
            VALUES (?, 'Kafka fixture', 'FIXED_AMOUNT', 1000, 1, 0, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 1 DAY, 1, 0)
            """).param(couponId).update();
        final var requests = new CouponIssueRequestRepository(jdbc);
        final var issue = new AcceptedCouponIssue(couponId, 90001L, 1);
        assertThat(requests.reserve(issue)).isEqualTo(Reservation.ACCEPTED);
        assertThat(requests.reserve(issue)).isEqualTo(Reservation.DUPLICATE);
        final long requestId = requests.lockPublications().getFirst();
        assertThat(requests.lockPending(requestId)).contains(issue);
        assertThat(requests.reserve(new AcceptedCouponIssue(couponId, 90002L, 1))).isEqualTo(Reservation.SOLD_OUT);
        requests.published(requestId);
        assertThat(requests.lockPublications()).isEmpty();
        requests.complete(requestId, couponId);
        assertThat(requests.lockPending(requestId)).isEmpty();
    }
}
