package com.book.core.onboarding.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.onboarding.application.command.GetOnboardingProgressCommand;
import com.book.core.onboarding.application.port.OnboardingOptionRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingAnswerRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingBookRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingRepositoryPort;
import com.book.core.onboarding.application.result.OnboardingProgressResult;
import com.book.core.onboarding.application.usecase.GetOnboardingProgressUseCase;
import com.book.core.onboarding.domain.OnboardingOption;
import com.book.core.onboarding.domain.OnboardingStatus;
import com.book.core.onboarding.domain.UserOnboarding;
import com.book.core.onboarding.domain.UserOnboardingAnswer;
import com.book.core.onboarding.domain.UserOnboardingBook;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetOnboardingProgressUseCaseTest {
    private final FakeUserOnboardingRepository userOnboardingRepository = new FakeUserOnboardingRepository();
    private final FakeUserOnboardingAnswerRepository answerRepository = new FakeUserOnboardingAnswerRepository();
    private final FakeOnboardingOptionRepository optionRepository = new FakeOnboardingOptionRepository();
    private final FakeUserOnboardingBookRepository bookRepository = new FakeUserOnboardingBookRepository();
    private final GetOnboardingProgressUseCase useCase = new GetOnboardingProgressUseCase(
            userOnboardingRepository, answerRepository, optionRepository, bookRepository);

    @Test
    void 진행중인_온보딩_상태를_반환한다() {
        userOnboardingRepository.byUserId.put(42L, new UserOnboarding(1L, 42L, OnboardingStatus.IN_PROGRESS, null));

        final OnboardingProgressResult result = useCase.execute(new GetOnboardingProgressCommand(42L));

        assertThat(result.status()).isEqualTo(OnboardingStatus.IN_PROGRESS);
        assertThat(result.completedAt()).isNull();
    }

    @Test
    void 완료된_온보딩_상태와_완료시각을_반환한다() {
        final LocalDateTime completedAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        userOnboardingRepository.byUserId.put(
                42L, new UserOnboarding(1L, 42L, OnboardingStatus.COMPLETED, completedAt));

        final OnboardingProgressResult result = useCase.execute(new GetOnboardingProgressCommand(42L));

        assertThat(result.status()).isEqualTo(OnboardingStatus.COMPLETED);
        assertThat(result.completedAt()).isEqualTo(completedAt);
    }

    @Test
    void 응답을_질문별로_그룹핑한다() {
        userOnboardingRepository.byUserId.put(42L, new UserOnboarding(1L, 42L, OnboardingStatus.IN_PROGRESS, null));
        answerRepository.byUserId.put(
                42L,
                List.of(
                        UserOnboardingAnswer.of(42L, 1L),
                        UserOnboardingAnswer.of(42L, 2L),
                        UserOnboardingAnswer.of(42L, 9L)));
        optionRepository.optionsById.put(1L, new OnboardingOption(1L, 1L, null, "morning", "아침", 1));
        optionRepository.optionsById.put(2L, new OnboardingOption(2L, 1L, null, "lunch", "점심", 2));
        optionRepository.optionsById.put(9L, new OnboardingOption(9L, 3L, null, "novel", "소설", 1));

        final OnboardingProgressResult result = useCase.execute(new GetOnboardingProgressCommand(42L));

        assertThat(result.answers()).hasSize(2);
        assertThat(result.answers().get(0).questionId()).isEqualTo(1L);
        assertThat(result.answers().get(0).optionIds()).containsExactly(1L, 2L);
        assertThat(result.answers().get(1).questionId()).isEqualTo(3L);
        assertThat(result.answers().get(1).optionIds()).containsExactly(9L);
    }

    @Test
    void 선택한_도서_목록을_반환한다() {
        userOnboardingRepository.byUserId.put(42L, new UserOnboarding(1L, 42L, OnboardingStatus.COMPLETED, null));
        bookRepository.byUserId.put(42L, List.of(UserOnboardingBook.of(42L, 30L), UserOnboardingBook.of(42L, 10L)));

        final OnboardingProgressResult result = useCase.execute(new GetOnboardingProgressCommand(42L));

        assertThat(result.bookIds()).containsExactly(10L, 30L);
    }

    @Test
    void 진행중인_온보딩이_없으면_예외를_던진다() {
        assertThatThrownBy(() -> useCase.execute(new GetOnboardingProgressCommand(42L)))
                .isInstanceOf(CoreException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ONBOARDING_NOT_FOUND);
    }

    private static final class FakeUserOnboardingRepository implements UserOnboardingRepositoryPort {
        private final Map<Long, UserOnboarding> byUserId = new HashMap<>();

        @Override
        public Optional<UserOnboarding> findActiveByUserId(final Long userId) {
            return Optional.ofNullable(byUserId.get(userId));
        }

        @Override
        public UserOnboarding save(final UserOnboarding userOnboarding) {
            return userOnboarding;
        }
    }

    private static final class FakeUserOnboardingAnswerRepository implements UserOnboardingAnswerRepositoryPort {
        private final Map<Long, List<UserOnboardingAnswer>> byUserId = new HashMap<>();

        @Override
        public List<UserOnboardingAnswer> findByUserId(final Long userId) {
            return byUserId.getOrDefault(userId, new ArrayList<>());
        }

        @Override
        public void deleteByUserIdAndOnboardingOptionIdIn(final Long userId, final List<Long> onboardingOptionIds) {}

        @Override
        public List<UserOnboardingAnswer> saveAll(final List<UserOnboardingAnswer> answers) {
            return answers;
        }
    }

    private static final class FakeOnboardingOptionRepository implements OnboardingOptionRepositoryPort {
        private final Map<Long, OnboardingOption> optionsById = new HashMap<>();

        @Override
        public List<OnboardingOption> findByQuestionId(final Long questionId) {
            return new ArrayList<>();
        }

        @Override
        public List<OnboardingOption> findAllByIdIn(final List<Long> ids) {
            return ids.stream()
                    .map(optionsById::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }
    }

    private static final class FakeUserOnboardingBookRepository implements UserOnboardingBookRepositoryPort {
        private final Map<Long, List<UserOnboardingBook>> byUserId = new HashMap<>();

        @Override
        public List<UserOnboardingBook> findByUserId(final Long userId) {
            return byUserId.getOrDefault(userId, new ArrayList<>());
        }

        @Override
        public void deleteByUserId(final Long userId) {}

        @Override
        public List<UserOnboardingBook> saveAll(final List<UserOnboardingBook> books) {
            return books;
        }
    }
}
