package com.book.core.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.auth.application.port.IssuedTokens;
import com.book.core.auth.application.port.RefreshSessionRepository;
import com.book.core.auth.application.port.RefreshTokenHasher;
import com.book.core.auth.application.port.RefreshTokenVerifier;
import com.book.core.auth.application.port.TokenIssuer;
import com.book.core.auth.domain.RefreshSession;
import com.book.core.user.application.port.UserRepositoryPort;
import com.book.core.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
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
    UserRepositoryPort userRepository;

    @Mock
    User user;

    @Mock
    TokenIssuer tokenIssuer;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(userRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(user));
        org.mockito.Mockito.lenient().when(user.isActive()).thenReturn(true);
    }

    @Test
    void reissuesValidRefreshTokenAndRevokesPreviousSession() {
        final RefreshSession currentSession =
            RefreshSession.restore(1L, 42L, "old-token-hash", Instant.parse("2030-01-05T00:00:00Z"), null);
        final Instant newRefreshExpiresAt = Instant.parse("2030-01-08T00:00:00Z");

        when(refreshTokenVerifier.verify("old-refresh-token")).thenReturn(42L);
        when(refreshTokenHasher.hash("old-refresh-token")).thenReturn("old-token-hash");
        when(refreshSessionRepository.findActiveByTokenHashForUpdate("old-token-hash")).thenReturn(Optional.of(currentSession));
        when(tokenIssuer.issue(42L)).thenReturn(new IssuedTokens("new-access-token", "new-refresh-token", newRefreshExpiresAt));
        when(refreshTokenHasher.hash("new-refresh-token")).thenReturn("new-token-hash");

        final var result = useCase().execute("old-refresh-token");

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(result.refreshExpiresAt()).isEqualTo(newRefreshExpiresAt);
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
    void rejectsMissingActiveSession() {
        when(refreshTokenVerifier.verify("refresh-token")).thenReturn(42L);
        when(refreshTokenHasher.hash("refresh-token")).thenReturn("token-hash");
        when(refreshSessionRepository.findActiveByTokenHashForUpdate("token-hash")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute("refresh-token")).isInstanceOf(BusinessException.class);

        verifyNoInteractions(tokenIssuer);
    }

    @Test
    void rejectsSessionOwnedByAnotherUser() {
        final RefreshSession session = RefreshSession.restore(1L, 99L, "token-hash", Instant.parse("2030-01-05T00:00:00Z"), null);

        when(refreshTokenVerifier.verify("refresh-token")).thenReturn(42L);
        when(refreshTokenHasher.hash("refresh-token")).thenReturn("token-hash");
        when(refreshSessionRepository.findActiveByTokenHashForUpdate("token-hash")).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> useCase().execute("refresh-token")).isInstanceOf(BusinessException.class);

        verifyNoInteractions(tokenIssuer);
    }

    @Test
    void doesNotReadSessionWhenJwtVerificationFails() {
        final BusinessException exception = new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);

        when(refreshTokenVerifier.verify("invalid-refresh-token")).thenThrow(exception);

        assertThatThrownBy(() -> useCase().execute("invalid-refresh-token")).isSameAs(exception);

        verifyNoInteractions(refreshTokenHasher, refreshSessionRepository, tokenIssuer);
    }

    @Test
    void rejectsInactiveUserWithoutChangingSessions() {
        when(refreshTokenVerifier.verify("refresh-token")).thenReturn(42L);
        when(user.isActive()).thenReturn(false);

        assertRestrictedUserRejected();
    }

    @Test
    void rejectsWithdrawalRequestedUserWithoutChangingSessions() {
        when(refreshTokenVerifier.verify("refresh-token")).thenReturn(42L);
        when(user.isWithdrawalRequested()).thenReturn(true);

        assertRestrictedUserRejected();
    }

    @Test
    void rejectsMissingUserWithoutChangingSessions() {
        when(refreshTokenVerifier.verify("refresh-token")).thenReturn(42L);
        when(userRepository.findByIdForUpdate(42L)).thenReturn(Optional.empty());

        assertRestrictedUserRejected();
    }

    private void assertRestrictedUserRejected() {
        assertThatThrownBy(() -> useCase().execute("refresh-token")).isInstanceOfSatisfying(BusinessException.class,
            (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
        verifyNoInteractions(refreshTokenHasher, refreshSessionRepository, tokenIssuer);
    }

    private AuthReissueUseCase useCase() {
        return new AuthReissueUseCase(refreshTokenVerifier, refreshTokenHasher, refreshSessionRepository, tokenIssuer, CLOCK,
            userRepository);
    }
}
