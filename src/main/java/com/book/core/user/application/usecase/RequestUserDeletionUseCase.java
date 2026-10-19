package com.book.core.user.application.usecase;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.user.application.port.RefreshSessionRevokerPort;
import com.book.core.user.application.port.UserRepositoryPort;
import com.book.core.user.domain.User;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RequestUserDeletionUseCase {
    private final UserRepositoryPort userRepository;
    private final RefreshSessionRevokerPort refreshSessionRevoker;
    private final Clock clock;

    @Transactional
    public void execute(final Long userId) {
        final User user = userRepository.findByIdForUpdate(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        user.requestUserDeletion(LocalDateTime.now(clock));
        refreshSessionRevoker.revokeAll(userId);
    }
}
