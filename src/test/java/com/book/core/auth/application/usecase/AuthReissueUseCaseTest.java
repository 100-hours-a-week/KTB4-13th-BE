package com.book.core.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.BusinessException;
import com.book.core.auth.application.port.IssuedTokens;
import com.book.core.auth.application.port.RefreshSessionRepository;
import com.book.core.auth.application.port.RefreshTokenHasher;
import com.book.core.auth.application.port.RefreshTokenVerifier;
import com.book.core.auth.application.port.TokenIssuer;
import com.book.core.auth.domain.RefreshSession;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthReissueUseCaseTest {
    private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    RefreshTokenVerifier refreshTokenVerifier;

    @Mock
    RefreshTokenHasher refreshTokenHasher;

    @Mock
    RefreshSessionRepository refreshSessionRepository;

    @Mock
    TokenIssuer tokenIssuer;

    @Test
    void 유효한_Refresh_Token이면_기존_session을_폐기하고_새_token과_session을_발급한다() {
        final RefreshSession currentSession =
                RefreshSession.restore(1L, 42L, "old-token-hash", Instant.parse("2030-01-05T00:00:00Z"), null);

        final Instant newRefreshExpiresAt = Instant.parse("2030-01-08T00:00:00Z");

        when(refreshTokenVerifier.verify("old-refresh-token")).thenReturn(42L);
        when(refreshTokenHasher.hash("old-refresh-token")).thenReturn("old-token-hash");
        when(refreshSessionRepository.findActiveByTokenHashForUpdate("old-token-hash"))
                .thenReturn(Optional.of(currentSession));
        when(tokenIssuer.issue(42L))
                .thenReturn(new IssuedTokens("new-access-token", "new-refresh-token", newRefreshExpiresAt));
        when(refreshTokenHasher.hash("new-refresh-token")).thenReturn("new-token-hash");

        final var result = useCase().execute("old-refresh-token");

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");

        assertThat(currentSession.revokedAt()).isEqualTo(NOW);

        final ArgumentCaptor<RefreshSession> sessionCaptor = ArgumentCaptor.forClass(RefreshSession.class);

        verify(refreshSessionRepository, times(2)).save(sessionCaptor.capture());

        final RefreshSession newSession = sessionCaptor.getAllValues().get(1);
        assertThat(newSession.userId()).isEqualTo(42L);
        assertThat(newSession.tokenHash()).isEqualTo("new-token-hash");
        assertThat(newSession.expiresAt()).isEqualTo(newRefreshExpiresAt);
        assertThat(newSession.revokedAt()).isNull();
    }

    @Test
    void 활성_Refresh_Session이_없으면_재발급하지_않는다() {
        when(refreshTokenVerifier.verify("refresh-token")).thenReturn(42L);
        when(refreshTokenHasher.hash("refresh-token")).thenReturn("token-hash");
        when(refreshSessionRepository.findActiveByTokenHashForUpdate("token-hash"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute("refresh-token")).isInstanceOf(BusinessException.class);

        verifyNoInteractions(tokenIssuer);
    }

    @Test
    void JWT의_userId와_session의_userId가_다르면_재발급하지_않는다() {
        final RefreshSession session =
                RefreshSession.restore(1L, 99L, "token-hash", Instant.parse("2030-01-05T00:00:00Z"), null);

        when(refreshTokenVerifier.verify("refresh-token")).thenReturn(42L);
        when(refreshTokenHasher.hash("refresh-token")).thenReturn("token-hash");
        when(refreshSessionRepository.findActiveByTokenHashForUpdate("token-hash"))
                .thenReturn(Optional.of(session));

        assertThatThrownBy(() -> useCase().execute("refresh-token")).isInstanceOf(BusinessException.class);

        verifyNoInteractions(tokenIssuer);
    }

    @Test
    void Refresh_Token_검증이_실패하면_session을_조회하지_않는다() {
        final BusinessException exception =
                new BusinessException(com.book.core.auth.domain.exception.AuthErrorCode.INVALID_REFRESH_TOKEN);

        when(refreshTokenVerifier.verify("invalid-refresh-token")).thenThrow(exception);

        assertThatThrownBy(() -> useCase().execute("invalid-refresh-token")).isSameAs(exception);

        verifyNoInteractions(refreshTokenHasher, refreshSessionRepository, tokenIssuer);
    }

    private AuthReissueUseCase useCase() {
        return new AuthReissueUseCase(
                refreshTokenVerifier, refreshTokenHasher, refreshSessionRepository, tokenIssuer, CLOCK);
    }
}
