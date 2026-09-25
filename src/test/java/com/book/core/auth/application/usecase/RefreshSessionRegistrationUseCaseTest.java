package com.book.core.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.auth.application.port.RefreshSessionRepository;
import com.book.core.auth.application.port.RefreshTokenHasher;
import com.book.core.auth.domain.RefreshSession;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshSessionRegistrationUseCaseTest {
    private static final Long USER_ID = 42L;
    private static final Instant NOW = Instant.parse("2030-01-02T03:04:05Z");
    private static final Instant EXPIRES_AT = Instant.parse("2030-01-09T03:04:05Z");

    @Mock
    RefreshSessionRepository refreshSessionRepository;

    @Mock
    RefreshTokenHasher refreshTokenHasher;

    @Test
    void refresh_token을_hash한_새_session을_저장한다() {
        when(refreshTokenHasher.hash("refresh-token")).thenReturn("token-hash");
        when(refreshSessionRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.empty());

        useCase().execute(USER_ID, "refresh-token", EXPIRES_AT);

        final var sessionCaptor = ArgumentCaptor.forClass(RefreshSession.class);
        verify(refreshSessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().userId()).isEqualTo(USER_ID);
        assertThat(sessionCaptor.getValue().tokenHash()).isEqualTo("token-hash");
        assertThat(sessionCaptor.getValue().tokenHash()).isNotEqualTo("refresh-token");
        assertThat(sessionCaptor.getValue().expiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(sessionCaptor.getValue().isActive()).isTrue();
    }

    @Test
    void 기존_active_session을_revoke한_뒤_새_session을_저장한다() {
        final RefreshSession existing =
                RefreshSession.restore(1L, USER_ID, "old-token-hash", EXPIRES_AT.minusSeconds(1), null);
        when(refreshTokenHasher.hash("new-refresh-token")).thenReturn("new-token-hash");
        when(refreshSessionRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(existing));

        useCase().execute(USER_ID, "new-refresh-token", EXPIRES_AT);

        final var sessionCaptor = ArgumentCaptor.forClass(RefreshSession.class);
        final InOrder saves = inOrder(refreshSessionRepository);
        saves.verify(refreshSessionRepository).findActiveByUserId(USER_ID);
        saves.verify(refreshSessionRepository).save(existing);
        saves.verify(refreshSessionRepository).save(sessionCaptor.capture());
        assertThat(existing.isActive()).isFalse();
        assertThat(existing.revokedAt()).isEqualTo(NOW);
        assertThat(sessionCaptor.getValue().tokenHash()).isEqualTo("new-token-hash");
        assertThat(sessionCaptor.getValue().isActive()).isTrue();
    }

    private RefreshSessionRegistrationUseCase useCase() {
        return new RefreshSessionRegistrationUseCase(
                refreshSessionRepository, refreshTokenHasher, Clock.fixed(NOW, ZoneOffset.UTC));
    }
}
