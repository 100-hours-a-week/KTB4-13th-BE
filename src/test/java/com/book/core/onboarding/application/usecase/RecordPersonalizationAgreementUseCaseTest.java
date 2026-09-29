package com.book.core.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.onboarding.application.command.RecordPersonalizationAgreementCommand;
import com.book.core.onboarding.application.port.TermRepositoryPort;
import com.book.core.onboarding.application.port.UserTermAgreementRepositoryPort;
import com.book.core.onboarding.domain.Term;
import com.book.core.onboarding.domain.TermAgreementAction;
import com.book.core.onboarding.domain.TermType;
import com.book.core.onboarding.domain.UserTermAgreement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class RecordPersonalizationAgreementUseCaseTest {
    private static final Long USER_ID = 42L;
    private static final Long TERM_ID = 7L;

    private final FakeTermRepository termRepository = new FakeTermRepository();
    private final FakeUserTermAgreementRepository agreementRepository = new FakeUserTermAgreementRepository();
    private final RecordPersonalizationAgreementUseCase useCase =
        new RecordPersonalizationAgreementUseCase(termRepository, agreementRepository);

    @Test
    void 활성_개인화_약관에_동의한_적이_없으면_AGREE_이력을_저장한다() {
        termRepository.activeTerm = personalizationTerm();

        useCase.execute(new RecordPersonalizationAgreementCommand(USER_ID));

        assertThat(agreementRepository.saved).singleElement().satisfies(agreement -> {
            assertThat(agreement.userId()).isEqualTo(USER_ID);
            assertThat(agreement.termId()).isEqualTo(TERM_ID);
            assertThat(agreement.action()).isEqualTo(TermAgreementAction.AGREE);
            assertThat(agreement.agreedAt()).isNotNull();
        });
    }

    @Test
    void 이미_동의했으면_저장하지_않고_정상_종료한다() {
        termRepository.activeTerm = personalizationTerm();
        agreementRepository.agreed = true;

        useCase.execute(new RecordPersonalizationAgreementCommand(USER_ID));

        assertThat(agreementRepository.saved).isEmpty();
    }

    @Test
    void 활성_개인화_약관이_없으면_공통_서버_오류로_실패한다() {
        assertThatThrownBy(() -> useCase.execute(new RecordPersonalizationAgreementCommand(USER_ID))).isInstanceOf(CoreException.class)
            .satisfies(exception -> assertThat(((CoreException) exception).errorCode()).isEqualTo(ErrorCode.DEFAULT_ERROR));
        assertThat(agreementRepository.saved).isEmpty();
    }

    @Test
    void 동시_요청이_먼저_동의를_저장해_중복_제약에_걸리면_정상_종료한다() {
        termRepository.activeTerm = personalizationTerm();
        agreementRepository.concurrentAgreementOnSave = true;

        useCase.execute(new RecordPersonalizationAgreementCommand(USER_ID));

        assertThat(agreementRepository.saved).isEmpty();
    }

    @Test
    void 저장_중_제약_위반_뒤에도_동의가_없으면_예외를_전파한다() {
        termRepository.activeTerm = personalizationTerm();
        agreementRepository.failOnSave = true;

        assertThatThrownBy(() -> useCase.execute(new RecordPersonalizationAgreementCommand(USER_ID)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static Term personalizationTerm() {
        return new Term(TERM_ID, TermType.PERSONALIZED_RECOMMENDATION, "개인화 약관", "내용", "1.0", false, true, 1);
    }

    private static class FakeTermRepository implements TermRepositoryPort {
        Term activeTerm;

        @Override
        public Optional<Term> findActiveByTermType(final TermType termType) {
            return Optional.ofNullable(activeTerm).filter(term -> term.termType() == termType);
        }
    }

    private static class FakeUserTermAgreementRepository implements UserTermAgreementRepositoryPort {
        final List<UserTermAgreement> saved = new ArrayList<>();
        boolean agreed;
        boolean concurrentAgreementOnSave;
        boolean failOnSave;

        @Override
        public boolean existsAgreedByUserIdAndTermId(final Long userId, final Long termId) {
            return agreed;
        }

        @Override
        public UserTermAgreement save(final UserTermAgreement agreement) {
            if (concurrentAgreementOnSave) {
                agreed = true;
                throw new DataIntegrityViolationException("duplicate agreement");
            }
            if (failOnSave) {
                throw new DataIntegrityViolationException("unexpected constraint violation");
            }
            saved.add(agreement);
            return agreement;
        }
    }
}
