package com.book.core.onboarding.infrastructure.persistence.repository;

import com.book.core.onboarding.domain.ConsentType;
import com.book.core.onboarding.domain.UserConsent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserConsentJpaRepository extends JpaRepository<UserConsent, Long> {
    Optional<UserConsent> findByUserIdAndConsentTypeAndWithdrawnAtIsNull(final Long userId, final ConsentType consentType);
}
