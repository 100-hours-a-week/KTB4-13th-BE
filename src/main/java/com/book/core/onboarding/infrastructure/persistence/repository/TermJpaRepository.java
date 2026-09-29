package com.book.core.onboarding.infrastructure.persistence.repository;

import com.book.core.onboarding.domain.Term;
import com.book.core.onboarding.domain.TermType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface TermJpaRepository extends JpaRepository<Term, Long> {
    Optional<Term> findByTermTypeAndActiveTrue(final TermType termType);
}
