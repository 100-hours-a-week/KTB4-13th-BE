package com.book.core.user.application.usecase;

import com.book.core.user.application.port.UserRepositoryPort;
import com.book.core.user.domain.User;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FindWithdrawalTargetsUseCase {
    private final UserRepositoryPort userRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<Long> execute() {
        return userRepository.findWithdrawalTargetIds(User.withdrawalCompletableRequestedBefore(LocalDateTime.now(clock)));
    }
}
