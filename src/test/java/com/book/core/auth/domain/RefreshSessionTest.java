package com.book.core.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RefreshSessionTest {
    private static final Instant EXPIRES_AT = Instant.parse("2030-01-09T03:04:05Z");

    @Test
    void 새_RefreshSession은_active_상태다() {
        final RefreshSession session = RefreshSession.create(42L, "token-hash", EXPIRES_AT);

        assertThat(session.id()).isNull();
        assertThat(session.isActive()).isTrue();
        assertThat(session.revokedAt()).isNull();
    }

    @Test
    void revokedAt이_있는_RefreshSession은_inactive_상태다() {
        final Instant revokedAt = Instant.parse("2030-01-03T03:04:05Z");

        final RefreshSession session = RefreshSession.restore(1L, 42L, "token-hash", EXPIRES_AT, revokedAt);

        assertThat(session.isActive()).isFalse();
        assertThat(session.revokedAt()).isEqualTo(revokedAt);
    }

    @Test
    void revoke하면_inactive_상태가_된다() {
        final Instant revokedAt = Instant.parse("2030-01-03T03:04:05Z");
        final RefreshSession session = RefreshSession.create(42L, "token-hash", EXPIRES_AT);

        session.revoke(revokedAt);

        assertThat(session.isActive()).isFalse();
        assertThat(session.revokedAt()).isEqualTo(revokedAt);
    }
}
