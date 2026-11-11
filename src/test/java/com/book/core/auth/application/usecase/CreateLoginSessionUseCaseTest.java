package com.book.core.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.core.auth.application.port.IssuedTokens;
import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.auth.application.port.RefreshSessionRepository;
import com.book.core.auth.application.port.RefreshTokenHasher;
import com.book.core.auth.application.port.TokenIssuer;
import com.book.core.user.application.port.UserRepositoryPort;
import com.book.core.user.domain.User;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CreateLoginSessionUseCaseTest {
    @Test
    void locksUserThenReplacesSessionWithTokenHash() {
        final TokenIssuer issuer = mock(TokenIssuer.class);
        final RefreshTokenHasher hasher = mock(RefreshTokenHasher.class);
        final RefreshSessionRepository sessions = mock(RefreshSessionRepository.class);
        final UserRepositoryPort users = mock(UserRepositoryPort.class);
        final Instant expiry = Instant.parse("2030-01-01T00:00:00Z");
        when(issuer.issue(42L)).thenReturn(new IssuedTokens("access", "refresh", expiry));
        when(hasher.hash("refresh")).thenReturn("hash");
        when(users.findByIdForUpdate(42L)).thenReturn(Optional.of(User.restore(42L, "회원이름")));

        final var result = new CreateLoginSessionUseCase(issuer, sessions, hasher, users).execute(42L);

        final var order = inOrder(users, sessions);
        order.verify(users).findByIdForUpdate(42L);
        order.verify(sessions).saveLoginSession(42L, "hash", expiry);
        assertThat(result.refreshToken()).isEqualTo("refresh");
        assertThat(result.accessToken()).isEqualTo("access");
    }

    @Test
    void tokenIssueFailureDoesNotChangeSession() {
        final TokenIssuer issuer = mock(TokenIssuer.class);
        final RefreshTokenHasher hasher = mock(RefreshTokenHasher.class);
        final RefreshSessionRepository sessions = mock(RefreshSessionRepository.class);
        final UserRepositoryPort users = mock(UserRepositoryPort.class);
        final BusinessException error = new BusinessException(ErrorCode.TOKEN_ISSUE_FAILURE);
        when(users.findByIdForUpdate(42L)).thenReturn(Optional.of(User.restore(42L, "회원이름")));
        when(issuer.issue(42L)).thenThrow(error);
        assertThatThrownBy(() -> new CreateLoginSessionUseCase(issuer, sessions, hasher, users).execute(42L)).isSameAs(error);
        verifyNoInteractions(hasher, sessions);
    }

    @Test
    void withdrawalPendingRejectsLoginBeforeIssuingTokens() {
        final TokenIssuer issuer = mock(TokenIssuer.class);
        final RefreshTokenHasher hasher = mock(RefreshTokenHasher.class);
        final RefreshSessionRepository sessions = mock(RefreshSessionRepository.class);
        final UserRepositoryPort users = mock(UserRepositoryPort.class);
        final User user = User.restore(42L, "회원이름");
        user.requestUserDeletion(LocalDateTime.of(2030, 1, 1, 0, 0));
        when(users.findByIdForUpdate(42L)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> new CreateLoginSessionUseCase(issuer, sessions, hasher, users).execute(42L)).isInstanceOfSatisfying(
            BusinessException.class,
            (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.USER_WITHDRAWAL_PENDING));
        verifyNoInteractions(issuer, hasher, sessions);
    }
}
