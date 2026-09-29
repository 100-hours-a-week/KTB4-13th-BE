package com.book.core.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.onboarding.application.command.UpdatePersonalizedRecommendationConsentCommand;
import com.book.core.onboarding.application.port.TermRepositoryPort;
import com.book.core.onboarding.application.port.UserTermAgreementRepositoryPort;
import com.book.core.onboarding.domain.Term;
import com.book.core.onboarding.domain.TermAgreementAction;
import com.book.core.onboarding.domain.TermType;
import com.book.core.onboarding.domain.UserTermAgreement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UpdatePersonalizedRecommendationConsentUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final LocalDateTime NOW_LOCAL = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);
    private static final Long TERM_ID = 7L;

    private final FakeTermRepository termRepository = new FakeTermRepository();
    private final FakeUserTermAgreementRepository agreementRepository = new FakeUserTermAgreementRepository();
    private final UpdatePersonalizedRecommendationConsentUseCase useCase =
        new UpdatePersonalizedRecommendationConsentUseCase(termRepository, agreementRepository, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void 동의하면_활성_개인화_추천_약관에_AGREE_이력을_생성한다() {
        termRepository.activeTerm = activeTerm();

        final var result = useCase.execute(new UpdatePersonalizedRecommendationConsentCommand(42L, true));

        assertThat(result.consented()).isTrue();
        assertThat(result.agreedAt()).isEqualTo(NOW_LOCAL);
        assertThat(agreementRepository.all).singleElement().satisfies(agreement -> {
            assertThat(agreement.userId()).isEqualTo(42L);
            assertThat(agreement.termId()).isEqualTo(TERM_ID);
            assertThat(agreement.action()).isEqualTo(TermAgreementAction.AGREE);
            assertThat(agreement.agreedAt()).isEqualTo(NOW_LOCAL);
        });
    }

    @Test
    void 이미_동의했으면_새_이력을_만들지_않고_기존_동의_시각을_반환한다() {
        termRepository.activeTerm = activeTerm();
        final LocalDateTime firstAgreedAt = LocalDateTime.of(2025, 12, 1, 0, 0);
        agreementRepository.save(UserTermAgreement.agree(42L, TERM_ID, firstAgreedAt));

        final var result = useCase.execute(new UpdatePersonalizedRecommendationConsentCommand(42L, true));

        assertThat(result.consented()).isTrue();
        assertThat(result.agreedAt()).isEqualTo(firstAgreedAt);
        assertThat(agreementRepository.all).hasSize(1);
    }

    @Test
    void 동의하지_않으면_이력을_만들지_않고_미동의로_응답한다() {
        termRepository.activeTerm = activeTerm();

        final var result = useCase.execute(new UpdatePersonalizedRecommendationConsentCommand(42L, false));

        assertThat(result.consented()).isFalse();
        assertThat(result.agreedAt()).isNull();
        assertThat(agreementRepository.all).isEmpty();
    }

    @Test
    void V1은_철회가_없으므로_이미_동의한_사용자의_false_요청은_기존_동의를_유지한다() {
        termRepository.activeTerm = activeTerm();
        final LocalDateTime firstAgreedAt = LocalDateTime.of(2025, 12, 1, 0, 0);
        agreementRepository.save(UserTermAgreement.agree(42L, TERM_ID, firstAgreedAt));

        final var result = useCase.execute(new UpdatePersonalizedRecommendationConsentCommand(42L, false));

        assertThat(result.consented()).isTrue();
        assertThat(result.agreedAt()).isEqualTo(firstAgreedAt);
        assertThat(agreementRepository.all).hasSize(1);
    }

    @Test
    void 다른_사용자의_동의는_서로_섞이지_않는다() {
        termRepository.activeTerm = activeTerm();
        useCase.execute(new UpdatePersonalizedRecommendationConsentCommand(42L, true));

        final var otherUser = useCase.execute(new UpdatePersonalizedRecommendationConsentCommand(99L, false));

        assertThat(otherUser.consented()).isFalse();
        assertThat(agreementRepository.all).extracting(UserTermAgreement::userId).containsExactly(42L);
    }

    @Test
    void 활성_개인화_추천_약관이_없으면_동의를_저장하지_않고_예외를_던진다() {
        assertThatThrownBy(() -> useCase.execute(new UpdatePersonalizedRecommendationConsentCommand(42L, true)))
            .isInstanceOf(CoreException.class).satisfies(exception -> assertThat(((CoreException) exception).errorCode())
                .isEqualTo(ErrorCode.PERSONALIZED_RECOMMENDATION_TERM_NOT_FOUND));
        assertThat(agreementRepository.all).isEmpty();
    }

    @Test
    void 활성_개인화_추천_약관이_없어도_미동의_요청은_성공한다() {
        final var result = useCase.execute(new UpdatePersonalizedRecommendationConsentCommand(42L, false));

        assertThat(result.consented()).isFalse();
        assertThat(result.agreedAt()).isNull();
    }

    private static Term activeTerm() {
        return new Term(TERM_ID, TermType.PERSONALIZED_RECOMMENDATION, true);
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
