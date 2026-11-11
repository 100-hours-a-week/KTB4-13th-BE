package com.book.core.auth.application.usecase;

import com.book.core.auth.application.port.RefreshSessionRepository;
import com.book.core.user.application.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthLogoutUseCase {
    private final RefreshSessionRepository refreshSessionRepository;
    private final UserRepositoryPort userRepository;

    @Transactional
    public void execute(final Long userId) {
        if (userRepository.findByIdForUpdate(userId).isEmpty()) {
            return;
        }
        refreshSessionRepository.revoke(userId);
    }
}
