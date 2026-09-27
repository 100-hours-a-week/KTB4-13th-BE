package com.book.core.auth.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.book.core.auth.domain.RefreshSession;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class RefreshSessionRepositoryAdapterTest {
    @Test
    void 저장과_조회의_기술_예외를_변환하지_않고_그대로_전파한다() {
        final RefreshSessionJpaRepository jpaRepository = mock(RefreshSessionJpaRepository.class);
        final var adapter = new RefreshSessionRepositoryAdapter(jpaRepository);
        final var failure = new DataAccessResourceFailureException("unavailable");
        when(jpaRepository.saveAndFlush(any())).thenThrow(failure);
        when(jpaRepository.findByUserIdAndRevokedAtIsNull(42L)).thenThrow(failure);
        when(jpaRepository.findByTokenHashAndRevokedAtIsNull("token-hash")).thenThrow(failure);
        final RefreshSession session = RefreshSession.create(42L, "token-hash", Instant.parse("2030-01-09T03:04:05Z"));

        assertThatThrownBy(() -> adapter.save(session)).isSameAs(failure);
        assertThatThrownBy(() -> adapter.findActiveByUserId(42L)).isSameAs(failure);
        assertThatThrownBy(() -> adapter.findActiveByTokenHash("token-hash")).isSameAs(failure);
    }

    @Test
    void 저장_결과를_Domain으로_변환해_반환한다() {
        final RefreshSessionJpaRepository jpaRepository = mock(RefreshSessionJpaRepository.class);
        final var adapter = new RefreshSessionRepositoryAdapter(jpaRepository);
        final RefreshSession session = RefreshSession.create(42L, "token-hash", Instant.parse("2030-01-09T03:04:05Z"));
        when(jpaRepository.saveAndFlush(any())).thenReturn(new com.book.core.auth.infrastructure.persistence.entity.RefreshSessionEntity(1L,
            42L, "token-hash", Instant.parse("2030-01-09T03:04:05Z"), null));

        final RefreshSession saved = adapter.save(session);

        assertThat(saved.id()).isEqualTo(1L);
        assertThat(saved.userId()).isEqualTo(42L);
    }
}
