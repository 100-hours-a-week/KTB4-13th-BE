package com.book.core.onboarding.application.port;

public interface UserOnboardingDataRepositoryPort {
    void deleteAllByUserId(final Long userId);
}
