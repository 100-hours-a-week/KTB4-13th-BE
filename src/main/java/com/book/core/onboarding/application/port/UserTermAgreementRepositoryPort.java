package com.book.core.onboarding.application.port;

import com.book.core.onboarding.domain.UserTermAgreement;

public interface UserTermAgreementRepositoryPort {
    boolean existsAgreedByUserIdAndTermId(final Long userId, final Long termId);

    UserTermAgreement save(final UserTermAgreement agreement);
}
