package com.book.core.onboarding.infrastructure.persistence.repository;

import com.book.core.onboarding.application.port.ProfileSyncJobRepositoryPort;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class ProfileSyncJobRepositoryAdapter implements ProfileSyncJobRepositoryPort {
    private final JdbcTemplate jdbc;

    @Override
    public void enqueue(final Long userId, final Instant availableAt) {
        final String revision = UUID.randomUUID().toString();
        final Timestamp due = Timestamp.from(availableAt);
        jdbc.update("""
            INSERT INTO profile_sync_jobs (user_id, revision, available_at)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE revision = ?, available_at = ?, attempts = 0
            """, userId, revision, due, revision, due);
    }

    @Override
    public Optional<Job> findNext(final Instant now) {
        return jdbc.query("""
            SELECT user_id, revision, attempts
            FROM profile_sync_jobs
            WHERE available_at <= ?
            ORDER BY available_at, user_id
            LIMIT 1
            """, (row, index) -> new Job(row.getLong("user_id"), UUID.fromString(row.getString("revision")), row.getInt("attempts")),
            Timestamp.from(now)).stream().findFirst();
    }

    @Override
    public void complete(final Job job) {
        jdbc.update("DELETE FROM profile_sync_jobs WHERE user_id = ? AND revision = ?", job.userId(), job.revision().toString());
    }

    @Override
    public void retry(final Job job, final Instant availableAt) {
        jdbc.update("""
            UPDATE profile_sync_jobs SET available_at = ?, attempts = attempts + 1
            WHERE user_id = ? AND revision = ?
            """, Timestamp.from(availableAt), job.userId(), job.revision().toString());
    }
}
