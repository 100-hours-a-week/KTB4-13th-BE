package com.book.core.user.application.usecase;

import com.book.core.user.application.port.UserProviderRepositoryPort;
import com.book.core.user.application.port.UserRepositoryPort;
import com.book.core.user.domain.User;
import com.book.core.user.domain.UserProvider;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompleteWithdrawalUseCase {
    private final UserRepositoryPort userRepository;
    private final UserProviderRepositoryPort userProviderRepository;
    private final Clock clock;

    @Transactional
    public boolean execute(final Long userId) {
        final LocalDateTime now = LocalDateTime.now(clock);
        final User user = userRepository.findByIdForUpdate(userId).orElse(null);
        if (user == null || !user.isWithdrawalCompletable(now)) {
            return false;
        }
        final List<UserProvider> providers = userProviderRepository.findActiveByUserId(userId);
        if (providers.stream().anyMatch((final UserProvider provider) -> !provider.isUnlinkSettled(now))) {
            return false;
        }
        providers.forEach(UserProvider::withdraw);
        user.completeWithdrawal();
        return true;
    }
}
