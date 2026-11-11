package com.book.core.user.application.usecase;

import com.book.core.user.application.port.UserRepositoryPort;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckWithdrawalConfirmedUseCase {
    private final UserRepositoryPort userRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public boolean execute(final Long userId) {
        return userRepository.findById(userId).map((final var user) -> user.isWithdrawalConfirmed(LocalDateTime.now(clock))).orElse(true);
    }
}
