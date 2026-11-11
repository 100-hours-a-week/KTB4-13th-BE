package com.book.core.auth.application.usecase;

import com.book.core.auth.application.port.IssuedTokens;
import com.book.core.auth.application.port.RefreshSessionRepository;
import com.book.core.auth.application.port.RefreshTokenHasher;
import com.book.core.auth.application.port.TokenIssuer;
import com.book.core.auth.application.result.AuthLoginResult;
import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.user.application.port.UserRepositoryPort;
import com.book.core.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateLoginSessionUseCase {
    private final TokenIssuer tokenIssuer;
    private final RefreshSessionRepository refreshSessionRepository;
    private final RefreshTokenHasher refreshTokenHasher;
    private final UserRepositoryPort userRepository;

    @Transactional
    public AuthLoginResult execute(final Long userId) {
        final User user =
            userRepository.findByIdForUpdate(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_PROVIDER_USER_NOT_FOUND));
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.USER_PROVIDER_USER_INACTIVE);
        }
        if (user.isWithdrawalRequested()) {
            throw new BusinessException(ErrorCode.USER_WITHDRAWAL_PENDING);
        }
        final IssuedTokens tokens = tokenIssuer.issue(userId);
        refreshSessionRepository.saveLoginSession(userId, refreshTokenHasher.hash(tokens.refreshToken()), tokens.refreshExpiresAt());
        return new AuthLoginResult(tokens.accessToken(), tokens.refreshToken(), tokens.refreshExpiresAt());
    }
}
