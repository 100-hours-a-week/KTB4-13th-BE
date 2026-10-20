package com.book.core.user.application.usecase;

import com.book.core.user.application.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckUserRestrictionUseCase {
    private final UserRepositoryPort userRepository;

    @Transactional(readOnly = true)
    public boolean execute(final Long userId) {
        return userRepository.findById(userId).map((final var user) -> !user.isActive() || user.isWithdrawalRequested()).orElse(false);
    }
}
