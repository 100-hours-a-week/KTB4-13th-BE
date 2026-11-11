package com.book.core.auth.application.usecase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

import com.book.core.auth.application.port.RefreshSessionRepository;
import com.book.core.user.application.port.UserRepositoryPort;
import com.book.core.user.domain.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AuthLogoutUseCaseTest {
    @Test
    void revokesActiveSessionForUser() {
        final RefreshSessionRepository sessions = mock(RefreshSessionRepository.class);
        final UserRepositoryPort users = mock(UserRepositoryPort.class);
        when(users.findByIdForUpdate(42L)).thenReturn(Optional.of(User.restore(42L, "회원이름")));
        new AuthLogoutUseCase(sessions, users).execute(42L);
        final var order = inOrder(users, sessions);
        order.verify(users).findByIdForUpdate(42L);
        order.verify(sessions).revoke(42L);
    }

    @Test
    void missingUserEndsWithoutChangingSession() {
        final RefreshSessionRepository sessions = mock(RefreshSessionRepository.class);
        final UserRepositoryPort users = mock(UserRepositoryPort.class);
        when(users.findByIdForUpdate(42L)).thenReturn(Optional.empty());
        new AuthLogoutUseCase(sessions, users).execute(42L);
        verifyNoInteractions(sessions);
    }
}
