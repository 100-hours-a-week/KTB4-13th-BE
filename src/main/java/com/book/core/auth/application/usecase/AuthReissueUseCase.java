package com.book.core.auth.application.usecase;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.auth.application.port.IssuedTokens;
import com.book.core.auth.application.port.RefreshSessionRepository;
import com.book.core.auth.application.port.RefreshTokenHasher;
import com.book.core.auth.application.port.RefreshTokenVerifier;
import com.book.core.auth.application.port.TokenIssuer;
import com.book.core.auth.application.result.AuthReissueResult;
import com.book.core.auth.domain.RefreshSession;
import com.book.core.user.application.port.UserRepositoryPort;
import com.book.core.user.domain.User;
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
    private final UserRepositoryPort userRepository;

    @Transactional
    public AuthReissueResult execute(final String refreshToken) {
        final Long userId = refreshTokenVerifier.verify(refreshToken);
        final User user =
            userRepository.findByIdForUpdate(userId).orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        if (!user.isActive() || user.isWithdrawalRequested()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        final String tokenHash = refreshTokenHasher.hash(refreshToken);

        final RefreshSession session = refreshSessionRepository.findActiveByTokenHashForUpdate(tokenHash)
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (!session.userId().equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        final IssuedTokens issuedTokens = tokenIssuer.issue(userId);

        session.revoke(clock.instant());
        refreshSessionRepository.save(session);
        refreshSessionRepository
            .save(RefreshSession.create(userId, refreshTokenHasher.hash(issuedTokens.refreshToken()), issuedTokens.refreshExpiresAt()));

        return new AuthReissueResult(issuedTokens.accessToken(), issuedTokens.refreshToken(), issuedTokens.refreshExpiresAt());
    }
}
