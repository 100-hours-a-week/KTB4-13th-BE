package com.book.core.onboarding.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.onboarding.application.port.UserOnboardingDataRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class DeleteUserOnboardingDataUseCase {
    private final UserOnboardingDataRepositoryPort userOnboardingDataRepository;

    @Transactional
    public void execute(final Long userId) {
        userOnboardingDataRepository.deleteAllByUserId(userId);
    }
}
