package com.book.core.onboarding.application.port;

import com.book.core.onboarding.domain.UserOnboarding;
import java.util.Optional;

public interface UserOnboardingRepositoryPort {
    Optional<UserOnboarding> findActiveByUserId(final Long userId);

    Optional<UserOnboarding> findActiveByUserIdForUpdate(final Long userId);

    UserOnboarding save(final UserOnboarding userOnboarding);
}
