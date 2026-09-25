package com.book.core.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.auth.application.port.RefreshSessionRepository;
import com.book.core.auth.domain.RefreshSession;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthLogoutUseCaseTest {

    private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    RefreshSessionRepository refreshSessionRepository;

    @Test
    void 활성_Refresh_Session이_있으면_폐기한다() {
        final RefreshSession session =
                RefreshSession.restore(1L, 42L, "token-hash", Instant.parse("2030-01-05T00:00:00Z"), null);

        when(refreshSessionRepository.findActiveByUserId(42L)).thenReturn(Optional.of(session));

        useCase().execute(42L);

        assertThat(session.revokedAt()).isEqualTo(NOW);
        verify(refreshSessionRepository).save(session);
    }

    @Test
    void 활성_Refresh_Session이_없어도_정상_종료한다() {
        when(refreshSessionRepository.findActiveByUserId(42L)).thenReturn(Optional.empty());

        useCase().execute(42L);

        verify(refreshSessionRepository, never()).save(any());
    }

    private AuthLogoutUseCase useCase() {
        return new AuthLogoutUseCase(refreshSessionRepository, CLOCK);
    }
}
