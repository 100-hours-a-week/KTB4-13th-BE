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
    void 예약과_발행_기록을_남기고_실패한_예약도_재고에_포함한다() {
        final long couponId = 559001;
        jdbc.sql("""
            INSERT INTO coupon (id, name, type, discount, max_use_count, min_order_amount, starts_at, expired_at, total_quantity, issued_quantity)
            VALUES (?, 'Kafka fixture', 'FIXED_AMOUNT', 1000, 1, 0, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 1 DAY, 1, 0)
            """).param(couponId).update();
        final var requests = new CouponIssueRequestRepository(jdbc);
        final var issue = new AcceptedCouponIssue(couponId, 90001L, 1);
        assertThat(requests.reserve(issue)).isEqualTo(Reservation.ACCEPTED);
        assertThat(requests.reserve(issue)).isEqualTo(Reservation.DUPLICATE);
        final var publication = requests.lockPublications().getFirst();
        assertThat(requests.lockPending(publication.id(), 0)).contains(issue);
        requests.published(publication);
        assertThat(requests.lockPublications()).isEmpty();
        requests.fail(publication.id(), 0, "event", "failure");
        assertThat(requests.countPending(couponId)).isOne();
        assertThat(requests.reserve(new AcceptedCouponIssue(couponId, 90002L, 1))).isEqualTo(Reservation.SOLD_OUT);
        assertThat(requests.lockPending(publication.id(), 0)).isEmpty();

        jdbc.sql("UPDATE coupon_issue_request SET replay_requested = TRUE WHERE id = ?").param(publication.id()).update();
        final var replay = requests.lockReplays().getFirst();
        requests.published(replay);
        assertThat(requests.lockPending(publication.id(), 0)).isEmpty();
        assertThat(requests.lockPending(publication.id(), 1)).contains(issue);
        requests.complete(publication.id(), couponId);
        assertThat(requests.lockPending(publication.id(), 1)).isEmpty();
        assertThat(requests.countPending(couponId)).isZero();
    }
}
