package com.book.core.onboarding.application.port;

import com.book.core.onboarding.domain.TermAgreementAction;
import com.book.core.onboarding.domain.UserTermAgreement;
import java.util.Optional;

public interface UserTermAgreementRepositoryPort {
    Optional<UserTermAgreement> findByUserIdAndTermIdAndAction(final Long userId, final Long termId, final TermAgreementAction action);

    UserTermAgreement save(final UserTermAgreement agreement);
}
