package com.book.core.auth.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.auth.domain.RefreshSession;
import com.book.core.auth.infrastructure.persistence.entity.RefreshSessionEntity;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RefreshSessionPersistenceMapperTest {
    @Test
    void Domain을_Entity로_변환한다() {
        final Instant expiresAt = Instant.parse("2030-01-09T03:04:05Z");
        final RefreshSession session = RefreshSession.create(42L, "token-hash", expiresAt);

        final RefreshSessionEntity entity = RefreshSessionPersistenceMapper.toEntity(session);

        assertThat(entity.id()).isNull();
        assertThat(entity.userId()).isEqualTo(42L);
        assertThat(entity.tokenHash()).isEqualTo("token-hash");
        assertThat(entity.expiresAt()).isEqualTo(expiresAt);
        assertThat(entity.revokedAt()).isNull();
    }

    @Test
    void Entity를_Domain으로_복원한다() {
        final Instant expiresAt = Instant.parse("2030-01-09T03:04:05Z");
        final Instant revokedAt = Instant.parse("2030-01-03T03:04:05Z");
        final RefreshSessionEntity entity = new RefreshSessionEntity(1L, 42L, "token-hash", expiresAt, revokedAt);

        final RefreshSession session = RefreshSessionPersistenceMapper.toDomain(entity);

        assertThat(session.id()).isEqualTo(1L);
        assertThat(session.userId()).isEqualTo(42L);
        assertThat(session.tokenHash()).isEqualTo("token-hash");
        assertThat(session.expiresAt()).isEqualTo(expiresAt);
        assertThat(session.revokedAt()).isEqualTo(revokedAt);
    }
}
