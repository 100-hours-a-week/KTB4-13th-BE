package com.book.core.onboarding.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.onboarding.application.command.PutOnboardingAnswersCommand;
import com.book.core.onboarding.application.port.OnboardingOptionRepositoryPort;
import com.book.core.onboarding.application.port.OnboardingQuestionRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingAnswerRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingRepositoryPort;
import com.book.core.onboarding.application.usecase.SaveOnboardingAnswersUseCase;
import com.book.core.onboarding.domain.OnboardingOption;
import com.book.core.onboarding.domain.OnboardingQuestion;
import com.book.core.onboarding.domain.OnboardingStatus;
import com.book.core.onboarding.domain.UserOnboarding;
import com.book.core.onboarding.domain.UserOnboardingAnswer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SaveOnboardingAnswersUseCaseTest {
    private final FakeOnboardingQuestionRepository questionRepository = new FakeOnboardingQuestionRepository();
    private final FakeOnboardingOptionRepository optionRepository = new FakeOnboardingOptionRepository();
    private final FakeUserOnboardingAnswerRepository answerRepository = new FakeUserOnboardingAnswerRepository();
    private final FakeUserOnboardingRepository userOnboardingRepository = new FakeUserOnboardingRepository();
    private final SaveOnboardingAnswersUseCase useCase = new SaveOnboardingAnswersUseCase(
            questionRepository, optionRepository, answerRepository, userOnboardingRepository);

    @Test
    void 활성_온보딩이_없으면_새로_시작하고_답변을_저장한다() {
        questionRepository.questions.put(1L, new OnboardingQuestion(1L, "reading-time", "Q1", 1, 5, 1));
        optionRepository.optionsById.put(1L, new OnboardingOption(1L, 1L, null, "morning", "아침", 1));
        optionRepository.optionsByQuestionId.put(1L, List.of(optionRepository.optionsById.get(1L)));

        useCase.execute(new PutOnboardingAnswersCommand(42L, 1L, List.of(1L)));

        assertThat(userOnboardingRepository.byUserId.get(42L)).isNotNull();
        assertThat(userOnboardingRepository.byUserId.get(42L).onboardingStatus())
                .isEqualTo(OnboardingStatus.IN_PROGRESS);
        assertThat(answerRepository.savedAnswers)
                .extracting(UserOnboardingAnswer::onboardingOptionId)
                .containsExactly(1L);
    }

    @Test
    void 기존_답변을_새_답변으로_교체한다() {
        questionRepository.questions.put(1L, new OnboardingQuestion(1L, "reading-time", "Q1", 1, 5, 1));
        final OnboardingOption option1 = new OnboardingOption(1L, 1L, null, "morning", "아침", 1);
        final OnboardingOption option2 = new OnboardingOption(2L, 1L, null, "lunch", "점심", 2);
        optionRepository.optionsById.put(1L, option1);
        optionRepository.optionsById.put(2L, option2);
        optionRepository.optionsByQuestionId.put(1L, List.of(option1, option2));
        userOnboardingRepository.byUserId.put(42L, UserOnboarding.start(42L));

        useCase.execute(new PutOnboardingAnswersCommand(42L, 1L, List.of(2L)));

        assertThat(answerRepository.deletedOptionIds).containsExactlyInAnyOrder(1L, 2L);
        assertThat(answerRepository.savedAnswers)
                .extracting(UserOnboardingAnswer::onboardingOptionId)
                .containsExactly(2L);
    }

    @Test
    void 최소_선택_개수_미만이면_예외를_던진다() {
        questionRepository.questions.put(1L, new OnboardingQuestion(1L, "reading-time", "Q1", 2, 5, 1));

        assertThatThrownBy(() -> useCase.execute(new PutOnboardingAnswersCommand(42L, 1L, List.of())))
                .isInstanceOf(CoreException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ONBOARDING_ANSWER_SELECTION);
    }

    @Test
    void 최대_선택_개수_초과이면_예외를_던진다() {
        questionRepository.questions.put(1L, new OnboardingQuestion(1L, "reading-time", "Q1", 1, 1, 1));

        assertThatThrownBy(() -> useCase.execute(new PutOnboardingAnswersCommand(42L, 1L, List.of(1L, 2L))))
                .isInstanceOf(CoreException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ONBOARDING_ANSWER_SELECTION);
    }

    @Test
    void 중복된_선택지이면_커맨드_생성_시점에_예외를_던진다() {
        assertThatThrownBy(() -> new PutOnboardingAnswersCommand(42L, 1L, List.of(1L, 1L)))
                .isInstanceOf(CoreException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ONBOARDING_ANSWER_SELECTION);
    }

    @Test
    void 존재하지_않는_질문이면_예외를_던진다() {
        assertThatThrownBy(() -> useCase.execute(new PutOnboardingAnswersCommand(42L, 999L, List.of(1L))))
                .isInstanceOf(CoreException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ONBOARDING_QUESTION_NOT_FOUND);
    }

    @Test
    void 존재하지_않는_선택지이면_예외를_던진다() {
        questionRepository.questions.put(1L, new OnboardingQuestion(1L, "reading-time", "Q1", 1, 5, 1));

        assertThatThrownBy(() -> useCase.execute(new PutOnboardingAnswersCommand(42L, 1L, List.of(999L))))
                .isInstanceOf(CoreException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ONBOARDING_OPTION_NOT_FOUND);
    }

    @Test
    void 다른_질문의_선택지이면_예외를_던진다() {
        questionRepository.questions.put(1L, new OnboardingQuestion(1L, "reading-time", "Q1", 1, 5, 1));
        optionRepository.optionsById.put(6L, new OnboardingOption(6L, 2L, null, "publisher", "출판사", 1));

        assertThatThrownBy(() -> useCase.execute(new PutOnboardingAnswersCommand(42L, 1L, List.of(6L))))
                .isInstanceOf(CoreException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ONBOARDING_OPTION_QUESTION_MISMATCH);
    }

    @Test
    void 선행_질문_응답이_없으면_예외를_던진다() {
        questionRepository.questions.put(4L, new OnboardingQuestion(4L, "subcategory", "Q4", 1, 9, 4));
        optionRepository.optionsById.put(23L, new OnboardingOption(23L, 4L, 9L, "novel-thriller", "추리", 1));

        assertThatThrownBy(() -> useCase.execute(new PutOnboardingAnswersCommand(42L, 4L, List.of(23L))))
                .isInstanceOf(CoreException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ONBOARDING_PARENT_QUESTION_NOT_ANSWERED);
    }

    private static final class FakeOnboardingQuestionRepository implements OnboardingQuestionRepositoryPort {
        private final Map<Long, OnboardingQuestion> questions = new HashMap<>();

        @Override
        public Optional<OnboardingQuestion> findById(final Long id) {
            return Optional.ofNullable(questions.get(id));
        }

        @Override
        public Optional<OnboardingQuestion> findFirstByDisplayOrderGreaterThan(final int displayOrder) {
            return Optional.empty();
        }
    }

    private static final class FakeOnboardingOptionRepository implements OnboardingOptionRepositoryPort {
        private final Map<Long, OnboardingOption> optionsById = new HashMap<>();
        private final Map<Long, List<OnboardingOption>> optionsByQuestionId = new HashMap<>();

        @Override
        public List<OnboardingOption> findByQuestionId(final Long questionId) {
            return optionsByQuestionId.getOrDefault(questionId, new ArrayList<>());
        }

        @Override
        public List<OnboardingOption> findAllByIdIn(final List<Long> ids) {
            return ids.stream()
                    .map(optionsById::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }
    }

    private static final class FakeUserOnboardingAnswerRepository implements UserOnboardingAnswerRepositoryPort {
        private final Map<Long, List<UserOnboardingAnswer>> byUserId = new HashMap<>();
        private List<Long> deletedOptionIds = List.of();
        private List<UserOnboardingAnswer> savedAnswers = List.of();

        @Override
        public List<UserOnboardingAnswer> findByUserId(final Long userId) {
            return byUserId.getOrDefault(userId, new ArrayList<>());
        }

        @Override
        public void deleteByUserIdAndOnboardingOptionIdIn(final Long userId, final List<Long> onboardingOptionIds) {
            deletedOptionIds = onboardingOptionIds;
        }

        @Override
        public List<UserOnboardingAnswer> saveAll(final List<UserOnboardingAnswer> answers) {
            savedAnswers = answers;
            return answers;
        }
    }

    private static final class FakeUserOnboardingRepository implements UserOnboardingRepositoryPort {
        private final Map<Long, UserOnboarding> byUserId = new HashMap<>();

        @Override
        public Optional<UserOnboarding> findActiveByUserId(final Long userId) {
            return Optional.ofNullable(byUserId.get(userId));
        }

        @Override
        public UserOnboarding save(final UserOnboarding userOnboarding) {
            byUserId.put(userOnboarding.userId(), userOnboarding);
            return userOnboarding;
        }
    }
}
