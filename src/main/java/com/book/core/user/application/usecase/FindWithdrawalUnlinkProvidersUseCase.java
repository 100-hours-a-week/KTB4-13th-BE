package com.book.core.user.application.usecase;

import com.book.core.user.application.port.UserProviderRepositoryPort;
import com.book.core.user.application.port.UserRepositoryPort;
import com.book.core.user.domain.UserProvider;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FindWithdrawalUnlinkProvidersUseCase {
    private final UserRepositoryPort userRepository;
    private final UserProviderRepositoryPort userProviderRepository;
    private final Clock clock;

    @Transactional
    public Optional<List<UserProvider>> execute(final Long userId) {
        final boolean completable = userRepository.findByIdForUpdate(userId)
            .filter((final var user) -> user.isWithdrawalCompletable(LocalDateTime.now(clock))).isPresent();
        if (!completable) {
            return Optional.empty();
        }
        return Optional.of(userProviderRepository.findActiveByUserId(userId).stream()
            .filter((final UserProvider provider) -> !provider.isUnlinked()).toList());
    }
}
