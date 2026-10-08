package com.book.core.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.onboarding.application.port.AiPersonalizationProfileClient;
import com.book.core.onboarding.application.port.AiPersonalizationProfileRequest;
import com.book.core.onboarding.application.port.OnboardingOptionRepositoryPort;
import com.book.core.onboarding.application.port.TermRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingAnswerRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingBookRepositoryPort;
import com.book.core.onboarding.application.port.UserTermAgreementRepositoryPort;
import com.book.core.onboarding.domain.OnboardingOption;
import com.book.core.onboarding.domain.Term;
import com.book.core.onboarding.domain.TermType;
import com.book.core.onboarding.domain.UserOnboardingAnswer;
import com.book.core.onboarding.domain.UserOnboardingBook;
import com.book.core.onboarding.domain.UserTermAgreement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CreatePersonalizationProfileUseCaseTest {
    private static final Long USER_ID = 42L;
    private static final Long TERM_ID = 7L;

    private final FakeTermRepository termRepository = new FakeTermRepository();
    private final FakeAgreementRepository agreementRepository = new FakeAgreementRepository();
    private final FakeAnswerRepository answerRepository = new FakeAnswerRepository();
    private final FakeOptionRepository optionRepository = new FakeOptionRepository();
    private final FakeBookRepository bookRepository = new FakeBookRepository();
    private final FakeProfileClient profileClient = new FakeProfileClient();
    private final CreatePersonalizationProfileUseCase useCase = new CreatePersonalizationProfileUseCase(termRepository, agreementRepository,
        answerRepository, optionRepository, bookRepository, profileClient);

    @Test
    void 개인화_추천에_동의했으면_AI_취향_프로필_생성을_요청한다() {
        termRepository.activeTerm = personalizationTerm();
        agreementRepository.agreed = true;

        useCase.execute(USER_ID);

        assertThat(profileClient.requests).singleElement().satisfies(request -> assertThat(request.userId()).isEqualTo(USER_ID));
    }

    @Test
    void 개인화_추천에_동의하지_않았으면_AI를_호출하지_않는다() {
        termRepository.activeTerm = personalizationTerm();

        useCase.execute(USER_ID);

        assertThat(profileClient.requests).isEmpty();
    }

    @Test
    void 활성_개인화_약관이_없으면_AI를_호출하지_않는다() {
        agreementRepository.agreed = true;

        useCase.execute(USER_ID);

        assertThat(profileClient.requests).isEmpty();
    }

    @Test
    void 온보딩_답변은_질문별_선택지_내용으로_선택_도서는_ID로_매핑한다() {
        termRepository.activeTerm = personalizationTerm();
        agreementRepository.agreed = true;
        optionRepository.options.addAll(List.of(new OnboardingOption(4L, 1L, null, "before-sleep", "잠들기 전", 4),
            new OnboardingOption(1L, 1L, null, "morning", "아침, 하루를 시작할 때", 1), new OnboardingOption(7L, 2L, null, "bestseller", "베스트셀러", 2),
            new OnboardingOption(9L, 3L, null, "novel", "소설", 1), new OnboardingOption(24L, 4L, 9L, "novel-sf", "SF", 2)));
        answerRepository.answers.addAll(List.of(UserOnboardingAnswer.of(USER_ID, 24L), UserOnboardingAnswer.of(USER_ID, 4L),
            UserOnboardingAnswer.of(USER_ID, 9L), UserOnboardingAnswer.of(USER_ID, 1L), UserOnboardingAnswer.of(USER_ID, 7L)));
        bookRepository.books.addAll(List.of(UserOnboardingBook.of(USER_ID, 300L), UserOnboardingBook.of(USER_ID, 100L)));

        useCase.execute(USER_ID);

        assertThat(profileClient.requests).singleElement().isEqualTo(new AiPersonalizationProfileRequest(USER_ID,
            List.of("아침, 하루를 시작할 때", "잠들기 전"), List.of("베스트셀러"), List.of("소설"), List.of("SF"), List.of(100L, 300L)));
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

    private static class FakeAgreementRepository implements UserTermAgreementRepositoryPort {
        boolean agreed;

        @Override
        public boolean existsAgreedByUserIdAndTermId(final Long userId, final Long termId) {
            return agreed && TERM_ID.equals(termId);
        }

        @Override
        public void saveIfAbsent(final UserTermAgreement agreement) {
            throw new UnsupportedOperationException();
        }
    }

    private static class FakeAnswerRepository implements UserOnboardingAnswerRepositoryPort {
        final List<UserOnboardingAnswer> answers = new ArrayList<>();

        @Override
        public List<UserOnboardingAnswer> findByUserId(final Long userId) {
            return answers.stream().filter(answer -> answer.userId().equals(userId)).toList();
        }

        @Override
        public void deleteByUserIdAndOnboardingOptionIdIn(final Long userId, final List<Long> onboardingOptionIds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<UserOnboardingAnswer> saveAll(final List<UserOnboardingAnswer> answers) {
            throw new UnsupportedOperationException();
        }
    }

    private static class FakeOptionRepository implements OnboardingOptionRepositoryPort {
        final List<OnboardingOption> options = new ArrayList<>();

        @Override
        public List<OnboardingOption> findByQuestionId(final Long questionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<OnboardingOption> findAllByIdIn(final List<Long> ids) {
            return options.stream().filter(option -> ids.contains(option.id())).toList();
        }
    }

    private static class FakeBookRepository implements UserOnboardingBookRepositoryPort {
        final List<UserOnboardingBook> books = new ArrayList<>();

        @Override
        public List<UserOnboardingBook> findByUserId(final Long userId) {
            return books.stream().filter(book -> book.userId().equals(userId)).toList();
        }

        @Override
        public void deleteByUserId(final Long userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<UserOnboardingBook> saveAll(final List<UserOnboardingBook> books) {
            throw new UnsupportedOperationException();
        }
    }

    private static class FakeProfileClient implements AiPersonalizationProfileClient {
        final List<AiPersonalizationProfileRequest> requests = new ArrayList<>();

        @Override
        public void createProfile(final AiPersonalizationProfileRequest request) {
            requests.add(request);
        }
    }
}
