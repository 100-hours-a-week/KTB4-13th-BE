package com.book.core.user.application.usecase;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.user.application.port.UserProviderRepositoryPort;
import com.book.core.user.application.port.UserRepositoryPort;
import com.book.core.user.application.result.OAuthUserResult;
import com.book.core.user.domain.ProviderType;
import com.book.core.user.domain.User;
import com.book.core.user.domain.UserProvider;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FindOAuthUserUseCase {
    private final UserRepositoryPort userRepository;
    private final UserProviderRepositoryPort userProviderRepository;

    @Transactional(readOnly = true)
    public Optional<OAuthUserResult> execute(final ProviderType providerType, final String providerUserId) {
        return userProviderRepository.findActiveByProviderTypeAndProviderUserId(providerType, providerUserId).map(this::findActiveUser);
    }

    private OAuthUserResult findActiveUser(final UserProvider userProvider) {
        final User user =
            userRepository.findById(userProvider.userId()).orElseThrow(() -> new BusinessException(ErrorCode.USER_PROVIDER_USER_NOT_FOUND));
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.USER_PROVIDER_USER_INACTIVE);
        }
        return new OAuthUserResult(user.id(), user.isWithdrawalRequested());
    }
}
