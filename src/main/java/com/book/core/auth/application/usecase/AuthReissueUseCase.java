package com.book.core.auth.application.usecase;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.auth.application.port.IssuedTokens;
import com.book.core.auth.application.port.RefreshSessionRepository;
import com.book.core.auth.application.port.RefreshTokenHasher;
import com.book.core.auth.application.port.RefreshTokenVerifier;
import com.book.core.auth.application.port.TokenIssuer;
import com.book.core.auth.application.result.AuthReissueResult;
import com.book.core.user.application.port.UserRepositoryPort;
import com.book.core.user.domain.User;
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
    private final UserRepositoryPort userRepository;

    @Transactional
    public AuthReissueResult execute(final String refreshToken) {
        final Long userId = refreshTokenVerifier.verify(refreshToken);
        final User user =
            userRepository.findByIdForUpdate(userId).orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        if (!user.isActive() || user.isWithdrawalRequested()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        final IssuedTokens tokens = tokenIssuer.issue(userId);
        if (!refreshSessionRepository.rotate(userId, refreshTokenHasher.hash(refreshToken), refreshTokenHasher.hash(tokens.refreshToken()),
            tokens.refreshExpiresAt())) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        return new AuthReissueResult(tokens.accessToken(), tokens.refreshToken(), tokens.refreshExpiresAt());
    }
}
