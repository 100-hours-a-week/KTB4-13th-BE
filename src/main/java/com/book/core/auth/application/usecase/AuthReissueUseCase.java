package com.book.core.auth.application.usecase;

import com.book.common.exception.BusinessException;
import com.book.core.auth.application.port.IssuedTokens;
import com.book.core.auth.application.port.RefreshSessionRepository;
import com.book.core.auth.application.port.RefreshTokenHasher;
import com.book.core.auth.application.port.RefreshTokenVerifier;
import com.book.core.auth.application.port.TokenIssuer;
import com.book.core.auth.application.result.AuthReissueResult;
import com.book.core.auth.domain.RefreshSession;
import com.book.core.auth.domain.exception.AuthErrorCode;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthReissueUseCase {
    private final RefreshTokenVerifier refreshTokenVerifier;
    private final RefreshTokenHasher refreshTokenHasher;
    private final RefreshSessionRepository refreshSessionRepository;
    private final TokenIssuer tokenIssuer;
    private final Clock clock;

    @Transactional
    public AuthReissueResult execute(final String refreshToken) {
        final Long userId = refreshTokenVerifier.verify(refreshToken);
        final String tokenHash = refreshTokenHasher.hash(refreshToken);

        final RefreshSession session = refreshSessionRepository
                .findActiveByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        if (!session.userId().equals(userId)) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        final IssuedTokens issuedTokens = tokenIssuer.issue(userId);

        session.revoke(clock.instant());
        refreshSessionRepository.save(session);

        refreshSessionRepository.save(RefreshSession.create(
                userId, refreshTokenHasher.hash(issuedTokens.refreshToken()), issuedTokens.refreshExpiresAt()));

        return new AuthReissueResult(issuedTokens.accessToken(), issuedTokens.refreshToken());
    }
}
