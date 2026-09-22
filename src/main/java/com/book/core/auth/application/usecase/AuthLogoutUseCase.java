package com.book.core.auth.application.usecase;

import com.book.core.auth.application.port.RefreshSessionRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthLogoutUseCase {

    private final RefreshSessionRepository refreshSessionRepository;
    private final Clock clock;

    @Transactional
    public void execute(final Long userId) {
        refreshSessionRepository.findActiveByUserId(userId).ifPresent((final var session) -> {
            session.revoke(clock.instant());
            refreshSessionRepository.save(session);
        });
    }
}
