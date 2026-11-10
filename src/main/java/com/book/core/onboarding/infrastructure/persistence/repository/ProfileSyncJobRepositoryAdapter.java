package com.book.core.onboarding.infrastructure.persistence.repository;

import com.book.core.onboarding.application.port.ProfileSyncJobRepositoryPort;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class ProfileSyncJobRepositoryAdapter implements ProfileSyncJobRepositoryPort {
    private final JdbcTemplate jdbc;
    private final boolean kafkaEnabled;

    ProfileSyncJobRepositoryAdapter(final JdbcTemplate jdbc, @Value("${app.profile-sync.kafka-enabled:false}") final boolean kafkaEnabled) {
        this.jdbc = jdbc;
        this.kafkaEnabled = kafkaEnabled;
    }

    @Override
    public void enqueue(final Long userId, final Instant availableAt) {
        final String revision = UUID.randomUUID().toString();
        final Timestamp due = Timestamp.from(availableAt);
        Timestamp publishAt = null;
        if (kafkaEnabled) {
            publishAt = due;
        }
        jdbc.update("""
            INSERT INTO profile_sync_jobs (user_id, revision, available_at, kafka_publish_at)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE revision = ?, available_at = ?, attempts = 0, kafka_publish_at = ?
            """, userId, revision, due, publishAt, revision, due, publishAt);
    }

    @Override
    public Optional<Job> findNext(final Instant now) {
        return jdbc.query("""
            SELECT user_id, revision, attempts
            FROM profile_sync_jobs
            WHERE available_at <= ? AND (kafka_publish_at IS NULL OR ?)
            ORDER BY available_at, user_id
            LIMIT 1
            """, (row, index) -> new Job(row.getLong("user_id"), UUID.fromString(row.getString("revision")), row.getInt("attempts")),
            Timestamp.from(now), !kafkaEnabled).stream().findFirst();
    }

    @Override
    public Optional<UUID> claimForPublication(final Instant now, final Instant leaseUntil) {
        final Timestamp due = Timestamp.from(now);
        return jdbc.query("""
            SELECT revision FROM profile_sync_jobs
            WHERE kafka_publish_at <= ?
            ORDER BY kafka_publish_at, user_id
            LIMIT 1
            """, (row, index) -> UUID.fromString(row.getString("revision")), due).stream().findFirst().filter(revision -> jdbc.update("""
                UPDATE profile_sync_jobs SET kafka_publish_at = ?
                WHERE revision = ? AND kafka_publish_at <= ?
                """, Timestamp.from(leaseUntil), revision.toString(), due) == 1);
    }

    @Override
    public void markReady(final UUID revision) {
        jdbc.update("UPDATE profile_sync_jobs SET kafka_publish_at = NULL WHERE revision = ? AND kafka_publish_at IS NOT NULL",
            revision.toString());
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
