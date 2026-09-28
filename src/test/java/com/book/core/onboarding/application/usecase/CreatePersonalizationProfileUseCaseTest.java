package com.book.core.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.onboarding.application.port.OnboardingOptionRepositoryPort;
import com.book.core.onboarding.application.port.PersonalizationProfileClient;
import com.book.core.onboarding.application.port.PersonalizationProfileRequest;
import com.book.core.onboarding.application.port.PersonalizationProfileResult;
import com.book.core.onboarding.application.port.UserConsentRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingAnswerRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingBookRepositoryPort;
import com.book.core.onboarding.domain.ConsentType;
import com.book.core.onboarding.domain.OnboardingOption;
import com.book.core.onboarding.domain.UserConsent;
import com.book.core.onboarding.domain.UserOnboardingAnswer;
import com.book.core.onboarding.domain.UserOnboardingBook;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CreatePersonalizationProfileUseCaseTest {
    private final FakeUserConsentRepository consentRepository = new FakeUserConsentRepository();
    private final FakeAnswerRepository answerRepository = new FakeAnswerRepository();
    private final FakeOptionRepository optionRepository = new FakeOptionRepository();
    private final FakeBookRepository bookRepository = new FakeBookRepository();
    private final FakeProfileClient profileClient = new FakeProfileClient();
    private final CreatePersonalizationProfileUseCase useCase =
        new CreatePersonalizationProfileUseCase(consentRepository, answerRepository, optionRepository, bookRepository, profileClient);

    @Test
    void 활성_동의가_없으면_AI를_호출하지_않는다() {
        useCase.execute(42L);

        assertThat(profileClient.requests).isEmpty();
    }

    @Test
    void 활성_동의가_있으면_AI를_호출한다() {
        consentRepository.active(42L);
        seedAnswers(42L);

        useCase.execute(42L);

        assertThat(profileClient.requests).hasSize(1);
    }

    @Test
    void Q1부터_Q4까지_content로_매핑되고_Q5는_liked_book_ids로_매핑된다() {
        consentRepository.active(42L);
        seedAnswers(42L);
        bookRepository.books.put(42L, List.of(UserOnboardingBook.of(42L, 100L), UserOnboardingBook.of(42L, 50L)));

        useCase.execute(42L);

        final PersonalizationProfileRequest request = profileClient.requests.get(0);
        assertThat(request.readingTimes()).containsExactly("아침");
        assertThat(request.criteria()).containsExactly("평점");
        assertThat(request.categories()).containsExactly("소설");
        assertThat(request.tags()).containsExactly("SF");
        assertThat(request.likedBookIds()).containsExactly(50L, 100L);
    }

    @Test
    void 동일한_입력이면_동일한_idempotency_key를_생성한다() {
        consentRepository.active(42L);
        seedAnswers(42L);

        useCase.execute(42L);
        useCase.execute(42L);

        assertThat(profileClient.requests.get(0).idempotencyKey()).isEqualTo(profileClient.requests.get(1).idempotencyKey());
    }

    @Test
    void 저장_조회_순서가_달라도_정규화된_입력이_같으면_동일한_idempotency_key를_생성한다() {
        consentRepository.active(42L);
        seedAnswers(42L);
        bookRepository.books.put(42L, List.of(UserOnboardingBook.of(42L, 100L), UserOnboardingBook.of(42L, 50L)));
        useCase.execute(42L);

        answerRepository.answers.get(42L).sort((left, right) -> right.onboardingOptionId().compareTo(left.onboardingOptionId()));
        bookRepository.books.put(42L, List.of(UserOnboardingBook.of(42L, 50L), UserOnboardingBook.of(42L, 100L)));
        useCase.execute(42L);

        assertThat(profileClient.requests.get(0).idempotencyKey()).isEqualTo(profileClient.requests.get(1).idempotencyKey());
    }

    @Test
    void 입력이_바뀌면_idempotency_key도_바뀐다() {
        consentRepository.active(42L);
        seedAnswers(42L);
        useCase.execute(42L);
        final String firstKey = profileClient.requests.get(0).idempotencyKey();

        bookRepository.books.put(42L, List.of(UserOnboardingBook.of(42L, 999L)));
        useCase.execute(42L);
        final String secondKey = profileClient.requests.get(1).idempotencyKey();

        assertThat(firstKey).isNotEqualTo(secondKey);
    }

    @Test
    void idempotency_key는_userId를_포함한다() {
        consentRepository.active(42L);
        seedAnswers(42L);

        useCase.execute(42L);

        assertThat(profileClient.requests.get(0).idempotencyKey()).startsWith("profile:42:");
    }

    private void seedAnswers(final Long userId) {
        optionRepository.options.put(1L, new OnboardingOption(1L, 1L, null, "morning", "아침", 1));
        optionRepository.options.put(2L, new OnboardingOption(2L, 2L, null, "rating", "평점", 1));
        optionRepository.options.put(3L, new OnboardingOption(3L, 3L, null, "novel", "소설", 1));
        optionRepository.options.put(4L, new OnboardingOption(4L, 4L, 3L, "sf", "SF", 1));
        answerRepository.answers.put(userId, new ArrayList<>(List.of(UserOnboardingAnswer.of(userId, 1L),
            UserOnboardingAnswer.of(userId, 2L), UserOnboardingAnswer.of(userId, 3L), UserOnboardingAnswer.of(userId, 4L))));
    }

    private static class FakeUserConsentRepository implements UserConsentRepositoryPort {
        private final Map<Long, UserConsent> activeByUserId = new HashMap<>();

        void active(final Long userId) {
            activeByUserId.put(userId, UserConsent.create(userId, ConsentType.PERSONALIZED_RECOMMENDATION, "v1", LocalDateTime.now()));
        }

        @Override
        public Optional<UserConsent> findActiveByUserIdAndConsentType(final Long userId, final ConsentType consentType) {
            return Optional.ofNullable(activeByUserId.get(userId));
        }

        @Override
        public UserConsent save(final UserConsent userConsent) {
            activeByUserId.put(userConsent.userId(), userConsent);
            return userConsent;
        }
    }

    private static class FakeAnswerRepository implements UserOnboardingAnswerRepositoryPort {
        private final Map<Long, List<UserOnboardingAnswer>> answers = new HashMap<>();

        @Override
        public List<UserOnboardingAnswer> findByUserId(final Long userId) {
            return answers.getOrDefault(userId, List.of());
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
        private final Map<Long, OnboardingOption> options = new HashMap<>();

        @Override
        public List<OnboardingOption> findByQuestionId(final Long questionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<OnboardingOption> findAllByIdIn(final List<Long> ids) {
            return ids.stream().map(options::get).filter(Objects::nonNull).toList();
        }
    }

    private static class FakeBookRepository implements UserOnboardingBookRepositoryPort {
        private final Map<Long, List<UserOnboardingBook>> books = new HashMap<>();

        @Override
        public List<UserOnboardingBook> findByUserId(final Long userId) {
            return books.getOrDefault(userId, new ArrayList<>());
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

    private static class FakeProfileClient implements PersonalizationProfileClient {
        private final List<PersonalizationProfileRequest> requests = new ArrayList<>();

        @Override
        public PersonalizationProfileResult createProfile(final PersonalizationProfileRequest request) {
            requests.add(request);
            return new PersonalizationProfileResult(false, 1);
        }
    }
}
