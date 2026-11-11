package com.book.core.coupon.infrastructure.persistence.repository;

import com.book.core.coupon.application.port.IssuanceDltLogRepositoryPort;
import com.book.core.coupon.domain.IssuanceDltLog;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class IssuanceDltLogRepositoryAdapter implements IssuanceDltLogRepositoryPort {
    private final JdbcClient jdbc;

    @Override
    public void record(final IssuanceDltLog log) {
        jdbc.sql("""
            INSERT INTO issuance_dlt_log (dlt_topic, dlt_partition, dlt_offset, message_key, payload, error_type)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE id = id
            """).param(log.topic()).param(log.partition()).param(log.offset()).param(log.messageKey()).param(log.payload())
            .param(log.errorType()).update();
    }

    @Override
    public Optional<ReplayRequest> lockNextRequested() {
        return jdbc.sql("""
            SELECT id, message_key, payload
            FROM issuance_dlt_log
            WHERE replayed_at IS NULL AND replay_requested_at IS NOT NULL AND payload IS NOT NULL
            ORDER BY replay_requested_at, id
            LIMIT 1 FOR UPDATE SKIP LOCKED
            """).query(ReplayRequest.class).optional();
    }

    @Override
    public void markReplayed(final long id) {
        jdbc.sql("UPDATE issuance_dlt_log SET replayed_at = CURRENT_TIMESTAMP(6) WHERE id = ?").param(id).update();
    }
}
