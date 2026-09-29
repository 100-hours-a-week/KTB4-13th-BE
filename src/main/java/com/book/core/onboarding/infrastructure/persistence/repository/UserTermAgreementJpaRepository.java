package com.book.core.onboarding.infrastructure.persistence.repository;

import com.book.core.onboarding.domain.TermAgreementAction;
import com.book.core.onboarding.domain.UserTermAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserTermAgreementJpaRepository extends JpaRepository<UserTermAgreement, Long> {
    boolean existsByUserIdAndTermIdAndAction(final Long userId, final Long termId, final TermAgreementAction action);
}
