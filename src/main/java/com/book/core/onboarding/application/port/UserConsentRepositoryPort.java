package com.book.core.onboarding.application.port;

import com.book.core.onboarding.domain.ConsentType;
import com.book.core.onboarding.domain.UserConsent;
import java.util.Optional;

public interface UserConsentRepositoryPort {
    Optional<UserConsent> findActiveByUserIdAndConsentType(final Long userId, final ConsentType consentType);

    UserConsent save(final UserConsent userConsent);
}
