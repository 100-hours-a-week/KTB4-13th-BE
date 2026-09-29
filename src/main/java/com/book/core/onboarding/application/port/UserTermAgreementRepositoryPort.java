package com.book.core.onboarding.application.port;

import com.book.core.onboarding.domain.UserTermAgreement;

public interface UserTermAgreementRepositoryPort {
    boolean existsAgreedByUserIdAndTermId(final Long userId, final Long termId);

    /**
     * 같은 사용자·약관·action의 이력이 이미 있으면 저장하지 않는다. 호출부 트랜잭션 밖에서 호출해야 동시 저장의 중복 충돌을 흡수할 수 있다.
     */
    void saveIfAbsent(final UserTermAgreement agreement);
}
