package com.book.core.onboarding.application.port;

import com.book.core.onboarding.domain.Term;
import com.book.core.onboarding.domain.TermType;
import java.util.Optional;

public interface TermRepositoryPort {
    Optional<Term> findActiveByTermType(final TermType termType);
}
