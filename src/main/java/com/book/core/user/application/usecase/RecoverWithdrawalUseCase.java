package com.book.core.user.application.usecase;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.user.application.port.UserRepositoryPort;
import com.book.core.user.domain.User;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecoverWithdrawalUseCase {
    private final UserRepositoryPort userRepository;
    private final Clock clock;

    @Transactional
    public void execute(final Long userId) {
        final User user =
            userRepository.findByIdForUpdate(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_PROVIDER_USER_NOT_FOUND));
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.USER_PROVIDER_USER_INACTIVE);
        }
        if (user.isWithdrawalRequested()) {
            user.recoverWithdrawal(LocalDateTime.now(clock));
        }
    }
}
