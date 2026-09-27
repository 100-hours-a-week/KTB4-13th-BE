package com.book.core.auth.application.usecase;

import com.book.core.auth.application.port.RefreshSessionRepository;
import com.book.core.auth.application.port.RefreshTokenHasher;
import com.book.core.auth.domain.RefreshSession;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshSessionRegistrationUseCase {
    private final RefreshSessionRepository refreshSessionRepository;
    private final RefreshTokenHasher refreshTokenHasher;
    private final Clock clock;

    @Transactional
    public void execute(final Long userId, final String refreshToken, final Instant expiresAt) {
        final String tokenHash = refreshTokenHasher.hash(refreshToken);
        refreshSessionRepository.findActiveByUserId(userId).ifPresent((final var session) -> {
            session.revoke(clock.instant());
            refreshSessionRepository.save(session);
        });
        refreshSessionRepository.save(RefreshSession.create(userId, tokenHash, expiresAt));
    }
}
