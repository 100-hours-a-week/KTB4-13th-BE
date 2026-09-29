package com.book.core.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.onboarding.application.command.GetPersonalizedRecommendationConsentCommand;
import com.book.core.onboarding.application.port.TermRepositoryPort;
import com.book.core.onboarding.application.port.UserTermAgreementRepositoryPort;
import com.book.core.onboarding.domain.Term;
import com.book.core.onboarding.domain.TermAgreementAction;
import com.book.core.onboarding.domain.TermType;
import com.book.core.onboarding.domain.UserTermAgreement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetPersonalizedRecommendationConsentUseCaseTest {
    private static final Long TERM_ID = 7L;

    private final FakeTermRepository termRepository = new FakeTermRepository();
    private final FakeUserTermAgreementRepository agreementRepository = new FakeUserTermAgreementRepository();
    private final GetPersonalizedRecommendationConsentUseCase useCase =
        new GetPersonalizedRecommendationConsentUseCase(termRepository, agreementRepository);

    @Test
    void 활성_약관에_AGREE_이력이_있으면_consented_true와_agreedAt을_반환한다() {
        termRepository.activeTerm = new Term(TERM_ID, TermType.PERSONALIZED_RECOMMENDATION, true);
        final LocalDateTime agreedAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        agreementRepository.all.add(UserTermAgreement.agree(42L, TERM_ID, agreedAt));

        final var result = useCase.execute(new GetPersonalizedRecommendationConsentCommand(42L));

        assertThat(result.consented()).isTrue();
        assertThat(result.agreedAt()).isEqualTo(agreedAt);
    }

    @Test
    void AGREE_이력이_없으면_consented_false와_agreedAt_null을_반환한다() {
        termRepository.activeTerm = new Term(TERM_ID, TermType.PERSONALIZED_RECOMMENDATION, true);
        agreementRepository.all.add(UserTermAgreement.agree(99L, TERM_ID, LocalDateTime.of(2026, 1, 1, 0, 0)));

        final var result = useCase.execute(new GetPersonalizedRecommendationConsentCommand(42L));

        assertThat(result.consented()).isFalse();
        assertThat(result.agreedAt()).isNull();
    }

    @Test
    void 활성_개인화_추천_약관이_없으면_consented_false와_agreedAt_null을_반환한다() {
        final var result = useCase.execute(new GetPersonalizedRecommendationConsentCommand(42L));

        assertThat(result.consented()).isFalse();
        assertThat(result.agreedAt()).isNull();
    }

    private static class FakeTermRepository implements TermRepositoryPort {
        Term activeTerm;

        @Override
        public Optional<Term> findActiveByTermType(final TermType termType) {
            return Optional.ofNullable(activeTerm).filter(term -> term.termType() == termType);
        }
    }

    private static class FakeUserTermAgreementRepository implements UserTermAgreementRepositoryPort {
        final List<UserTermAgreement> all = new ArrayList<>();

        @Override
        public Optional<UserTermAgreement> findByUserIdAndTermIdAndAction(final Long userId, final Long termId,
            final TermAgreementAction action) {
            return all.stream()
                .filter(agreement -> agreement.userId().equals(userId) && agreement.termId().equals(termId) && agreement.action() == action)
                .findFirst();
        }

        @Override
        public UserTermAgreement save(final UserTermAgreement agreement) {
            all.add(agreement);
            return agreement;
        }
    }
}
