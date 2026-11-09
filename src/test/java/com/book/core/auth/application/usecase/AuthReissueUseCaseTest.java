package com.book.core.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.auth.application.port.IssuedTokens;
import com.book.core.auth.application.port.RefreshSessionRepository;
import com.book.core.auth.application.port.RefreshTokenHasher;
import com.book.core.auth.application.port.RefreshTokenVerifier;
import com.book.core.auth.application.port.TokenIssuer;
import com.book.core.user.application.port.UserRepositoryPort;
import com.book.core.user.domain.User;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AuthReissueUseCaseTest {
    private final RefreshTokenVerifier verifier = mock(RefreshTokenVerifier.class);
    private final RefreshTokenHasher hasher = mock(RefreshTokenHasher.class);
    private final RefreshSessionRepository sessions = mock(RefreshSessionRepository.class);
    private final TokenIssuer issuer = mock(TokenIssuer.class);
    private final UserRepositoryPort users = mock(UserRepositoryPort.class);
    private final Instant expiry = Instant.parse("2030-01-01T00:00:00Z");

    @Test
    void rotatesOnlyAfterLockingUser() {
        givenTokens();
        when(sessions.rotate(42L, "old-hash", "new-hash", expiry)).thenReturn(true);
        final var result = useCase().execute("old");
        final var order = inOrder(users, sessions);
        order.verify(users).findByIdForUpdate(42L);
        order.verify(sessions).rotate(42L, "old-hash", "new-hash", expiry);
        assertThat(result.refreshToken()).isEqualTo("new");
    }

    @Test
    void rejectsAlreadyConsumedToken() {
        givenTokens();
        assertThatThrownBy(() -> useCase().execute("old")).isInstanceOfSatisfying(BusinessException.class,
            (final var e) -> assertThat(e.errorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
    }

    @Test
    void invalidJwtDoesNotAccessSessionOrIssueTokens() {
        final BusinessException error = new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        when(verifier.verify("invalid")).thenThrow(error);
        assertThatThrownBy(() -> useCase().execute("invalid")).isSameAs(error);
        verifyNoInteractions(hasher, sessions, issuer, users);
    }

    @Test
    void rejectsMissingInactiveAndWithdrawingUsersWithoutIssuingTokens() {
        when(verifier.verify("old")).thenReturn(42L);
        final User inactive = User.restore(42L, "회원이름");
        inactive.completeWithdrawal();
        final User withdrawing = User.restore(42L, "회원이름");
        withdrawing.requestUserDeletion(LocalDateTime.of(2030, 1, 1, 0, 0));
        when(users.findByIdForUpdate(42L)).thenReturn(Optional.empty(), Optional.of(inactive), Optional.of(withdrawing));
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> useCase().execute("old")).isInstanceOfSatisfying(BusinessException.class,
                (final var e) -> assertThat(e.errorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
        }
        verifyNoInteractions(hasher, sessions, issuer);
    }

    private void givenTokens() {
        when(verifier.verify("old")).thenReturn(42L);
        when(users.findByIdForUpdate(42L)).thenReturn(Optional.of(User.restore(42L, "회원이름")));
        when(issuer.issue(42L)).thenReturn(new IssuedTokens("access", "new", expiry));
        when(hasher.hash("old")).thenReturn("old-hash");
        when(hasher.hash("new")).thenReturn("new-hash");
    }

    private AuthReissueUseCase useCase() {
        return new AuthReissueUseCase(verifier, hasher, sessions, issuer, users);
    }
}
