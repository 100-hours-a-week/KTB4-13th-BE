package com.book.core.auth.infrastructure.session;

import com.book.core.auth.application.usecase.AuthLogoutUseCase;
import com.book.core.user.application.port.RefreshSessionRevokerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class RefreshSessionRevokerAdapter implements RefreshSessionRevokerPort {
    private final AuthLogoutUseCase authLogoutUseCase;

    @Override
    public void revokeAll(final Long userId) {
        authLogoutUseCase.execute(userId);
    }
}
