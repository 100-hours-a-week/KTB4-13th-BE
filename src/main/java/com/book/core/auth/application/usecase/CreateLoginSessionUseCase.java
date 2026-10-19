package com.book.core.auth.application.usecase;

import com.book.core.auth.application.port.IssuedTokens;
import com.book.core.auth.application.port.RefreshSessionRepository;
import com.book.core.auth.application.port.RefreshTokenHasher;
import com.book.core.auth.application.port.TokenIssuer;
import com.book.core.auth.application.result.AuthLoginResult;
import com.book.core.auth.domain.RefreshSession;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateLoginSessionUseCase {
    private final TokenIssuer tokenIssuer;
    private final RefreshSessionRepository refreshSessionRepository;
    private final RefreshTokenHasher refreshTokenHasher;
    private final Clock clock;

    @Transactional
    public AuthLoginResult execute(final Long userId) {
        final IssuedTokens tokens = tokenIssuer.issue(userId);
        final String tokenHash = refreshTokenHasher.hash(tokens.refreshToken());
        refreshSessionRepository.findActiveByUserId(userId).ifPresent((final var session) -> {
            session.revoke(clock.instant());
            refreshSessionRepository.save(session);
        });
        refreshSessionRepository.save(RefreshSession.create(userId, tokenHash, tokens.refreshExpiresAt()));
        return new AuthLoginResult(tokens.accessToken(), tokens.refreshToken(), tokens.refreshExpiresAt());
    }
}
