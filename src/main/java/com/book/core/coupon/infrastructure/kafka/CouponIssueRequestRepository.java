package com.book.core.coupon.infrastructure.kafka;

import com.book.core.coupon.application.command.AcceptedCouponIssue;
import com.book.core.coupon.application.port.CouponIssueQueuePort;
import com.book.core.coupon.application.port.CouponIssuePendingPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("coupon-kafka")
@RequiredArgsConstructor
class CouponIssueRequestRepository implements CouponIssueQueuePort, CouponIssuePendingPort {
    private final JdbcClient jdbc;

    @Override
    @Transactional
    public Reservation reserve(final AcceptedCouponIssue issue) {
        final var remaining = jdbc.sql("""
            SELECT total_quantity - issued_quantity - reserved_quantity FROM coupon
            WHERE id = ? AND deleted_at IS NULL AND starts_at <= CURRENT_TIMESTAMP(6)
              AND expired_at > CURRENT_TIMESTAMP(6) FOR UPDATE
            """).param(issue.couponId()).query(Long.class).optional();
        if (remaining.isEmpty()) {
            return Reservation.UNAVAILABLE;
        }
        final boolean duplicate = jdbc.sql("""
            SELECT EXISTS(SELECT 1 FROM coupon_issue_request WHERE coupon_id = ? AND user_id = ?)
              OR EXISTS(SELECT 1 FROM owned_coupon WHERE coupon_id = ? AND user_id = ?)
            """).params(issue.couponId(), issue.userId(), issue.couponId(), issue.userId()).query(Boolean.class).single();
        if (duplicate) {
            return Reservation.DUPLICATE;
        }
        if (remaining.get() <= 0) {
            return Reservation.SOLD_OUT;
        }
        jdbc.sql("INSERT INTO coupon_issue_request(coupon_id, user_id, max_use_count) VALUES (?, ?, ?)")
            .params(issue.couponId(), issue.userId(), issue.maxUseCount()).update();
        jdbc.sql("UPDATE coupon SET reserved_quantity = reserved_quantity + 1 WHERE id = ?").param(issue.couponId()).update();
        return Reservation.ACCEPTED;
    }

    @Override
    public long countPending(final long couponId) {
        return jdbc.sql("SELECT COUNT(*) FROM coupon_issue_request WHERE coupon_id = ? AND completed_at IS NULL").param(couponId)
            .query(Long.class).single();
    }

    List<Publication> lockPublications() {
        return jdbc.sql("""
            SELECT id, generation FROM coupon_issue_request
            WHERE published_at IS NULL ORDER BY id LIMIT 64 FOR UPDATE SKIP LOCKED
            """).query(Publication.class).list();
    }

    List<Publication> lockReplays() {
        return jdbc.sql("""
            SELECT id, generation + 1 AS generation FROM coupon_issue_request
            WHERE replay_requested = TRUE AND failed_at IS NOT NULL AND completed_at IS NULL
            ORDER BY id LIMIT 64 FOR UPDATE SKIP LOCKED
            """).query(Publication.class).list();
    }

    void published(final Publication publication) {
        jdbc.sql("""
            UPDATE coupon_issue_request SET published_at = CURRENT_TIMESTAMP(6), generation = ?,
                failed_at = NULL, replay_requested = FALSE WHERE id = ?
            """).params(publication.generation(), publication.id()).update();
    }

    java.util.Optional<AcceptedCouponIssue> lockPending(final long id, final int generation) {
        return jdbc.sql("""
            SELECT coupon_id, user_id, max_use_count FROM coupon_issue_request
            WHERE id = ? AND generation = ? AND completed_at IS NULL AND failed_at IS NULL FOR UPDATE
            """).params(id, generation).query(AcceptedCouponIssue.class).optional();
    }

    void complete(final long id, final long couponId) {
        final int updated = jdbc.sql("UPDATE coupon SET reserved_quantity = reserved_quantity - 1 WHERE id = ? AND reserved_quantity > 0")
            .param(couponId).update();
        if (updated != 1) {
            throw new IllegalStateException("접수된 쿠폰의 예약 수량을 반영할 수 없습니다.");
        }
        jdbc.sql("UPDATE coupon_issue_request SET completed_at = CURRENT_TIMESTAMP(6) WHERE id = ?").param(id).update();
    }

    void fail(final long id, final int generation, final String payload, final String reason) {
        jdbc.sql("""
            UPDATE coupon_issue_request SET failed_at = CURRENT_TIMESTAMP(6), failure_reason = ?, failure_payload = ?
            WHERE id = ? AND generation = ? AND completed_at IS NULL AND failed_at IS NULL
            """).params(reason, payload, id, generation).update();
    }

    record Publication(long id, int generation) {
    }
}
