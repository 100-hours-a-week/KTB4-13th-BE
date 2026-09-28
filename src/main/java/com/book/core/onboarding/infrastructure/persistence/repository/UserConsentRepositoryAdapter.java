package com.book.core.onboarding.infrastructure.persistence.repository;

import com.book.core.onboarding.application.port.UserConsentRepositoryPort;
import com.book.core.onboarding.domain.ConsentType;
import com.book.core.onboarding.domain.UserConsent;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class UserConsentRepositoryAdapter implements UserConsentRepositoryPort {
    private final UserConsentJpaRepository jpaRepository;

    @Override
    public Optional<UserConsent> findActiveByUserIdAndConsentType(final Long userId, final ConsentType consentType) {
        return jpaRepository.findByUserIdAndConsentTypeAndWithdrawnAtIsNull(userId, consentType);
    }

    @Override
    public UserConsent save(final UserConsent userConsent) {
        return jpaRepository.save(userConsent);
    }
}
