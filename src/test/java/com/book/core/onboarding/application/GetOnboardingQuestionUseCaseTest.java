package com.book.core.onboarding.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.onboarding.application.command.GetOnboardingQuestionCommand;
import com.book.core.onboarding.application.port.OnboardingOptionRepositoryPort;
import com.book.core.onboarding.application.port.OnboardingQuestionRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingAnswerRepositoryPort;
import com.book.core.onboarding.application.result.OnboardingQuestionResult;
import com.book.core.onboarding.application.usecase.GetOnboardingQuestionUseCase;
import com.book.core.onboarding.domain.OnboardingOption;
import com.book.core.onboarding.domain.OnboardingQuestion;
import com.book.core.onboarding.domain.UserOnboardingAnswer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetOnboardingQuestionUseCaseTest {
    private final FakeOnboardingQuestionRepository questionRepository = new FakeOnboardingQuestionRepository();
    private final FakeOnboardingOptionRepository optionRepository = new FakeOnboardingOptionRepository();
    private final FakeUserOnboardingAnswerRepository answerRepository = new FakeUserOnboardingAnswerRepository();
    private final GetOnboardingQuestionUseCase useCase =
            new GetOnboardingQuestionUseCase(questionRepository, optionRepository, answerRepository);

    @Test
    void 질문과_선택지를_표시순서대로_반환한다() {
        questionRepository.questions.put(1L, new OnboardingQuestion(1L, "reading-time", "주로 언제 책을 읽으시나요?", 1, 5, 1));
        optionRepository.optionsByQuestionId.put(
                1L,
                List.of(
                        new OnboardingOption(2L, 1L, null, "lunch", "점심시간", 2),
                        new OnboardingOption(1L, 1L, null, "morning", "아침", 1)));

        final OnboardingQuestionResult result = useCase.execute(new GetOnboardingQuestionCommand(42L, 1L));

        assertThat(result.questionId()).isEqualTo(1L);
        assertThat(result.options()).extracting("optionId").containsExactly(2L, 1L);
    }

    @Test
    void 존재하지_않는_질문이면_예외를_던진다() {
        assertThatThrownBy(() -> useCase.execute(new GetOnboardingQuestionCommand(42L, 999L)))
                .isInstanceOf(CoreException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ONBOARDING_QUESTION_NOT_FOUND);
    }

    @Test
    void 다음_질문이_있으면_nextQuestionId를_반환한다() {
        questionRepository.questions.put(1L, new OnboardingQuestion(1L, "reading-time", "Q1", 1, 5, 1));
        questionRepository.questions.put(2L, new OnboardingQuestion(2L, "book-criteria", "Q2", 1, 3, 2));
        questionRepository.nextByDisplayOrder.put(1, Optional.of(questionRepository.questions.get(2L)));

        final OnboardingQuestionResult result = useCase.execute(new GetOnboardingQuestionCommand(42L, 1L));

        assertThat(result.nextQuestionId()).isEqualTo(2L);
    }

    @Test
    void 마지막_질문이면_nextQuestionId가_null이다() {
        questionRepository.questions.put(4L, new OnboardingQuestion(4L, "subcategory", "Q4", 1, 9, 4));
        questionRepository.nextByDisplayOrder.put(4, Optional.empty());

        final OnboardingQuestionResult result = useCase.execute(new GetOnboardingQuestionCommand(42L, 4L));

        assertThat(result.nextQuestionId()).isNull();
    }

    @Test
    void 선행_질문_응답이_없으면_하위_카테고리_질문_조회시_예외를_던진다() {
        questionRepository.questions.put(4L, new OnboardingQuestion(4L, "subcategory", "Q4", 1, 9, 4));
        optionRepository.optionsByQuestionId.put(
                4L,
                List.of(
                        new OnboardingOption(23L, 4L, 9L, "novel-thriller", "추리/스릴러", 1),
                        new OnboardingOption(28L, 4L, 10L, "humanities-psychology", "심리학", 1)));

        assertThatThrownBy(() -> useCase.execute(new GetOnboardingQuestionCommand(42L, 4L)))
                .isInstanceOf(CoreException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ONBOARDING_PARENT_QUESTION_NOT_ANSWERED);
    }

    @Test
    void 선행_질문에_응답한_대분류의_하위_선택지만_반환한다() {
        questionRepository.questions.put(4L, new OnboardingQuestion(4L, "subcategory", "Q4", 1, 9, 4));
        optionRepository.optionsByQuestionId.put(
                4L,
                List.of(
                        new OnboardingOption(23L, 4L, 9L, "novel-thriller", "추리/스릴러", 1),
                        new OnboardingOption(24L, 4L, 9L, "novel-sf", "SF", 2),
                        new OnboardingOption(28L, 4L, 10L, "humanities-psychology", "심리학", 1)));
        answerRepository.answersByUserId.put(42L, List.of(UserOnboardingAnswer.of(42L, 9L)));

        final OnboardingQuestionResult result = useCase.execute(new GetOnboardingQuestionCommand(42L, 4L));

        assertThat(result.options()).extracting("optionId").containsExactly(23L, 24L);
    }

    private static final class FakeOnboardingQuestionRepository implements OnboardingQuestionRepositoryPort {
        private final Map<Long, OnboardingQuestion> questions = new HashMap<>();
        private final Map<Integer, Optional<OnboardingQuestion>> nextByDisplayOrder = new HashMap<>();

        @Override
        public Optional<OnboardingQuestion> findById(final Long id) {
            return Optional.ofNullable(questions.get(id));
        }

        @Override
        public Optional<OnboardingQuestion> findFirstByDisplayOrderGreaterThan(final int displayOrder) {
            return nextByDisplayOrder.getOrDefault(displayOrder, Optional.empty());
        }
    }

    private static final class FakeOnboardingOptionRepository implements OnboardingOptionRepositoryPort {
        private final Map<Long, List<OnboardingOption>> optionsByQuestionId = new HashMap<>();

        @Override
        public List<OnboardingOption> findByQuestionId(final Long questionId) {
            return optionsByQuestionId.getOrDefault(questionId, new ArrayList<>());
        }

        @Override
        public List<OnboardingOption> findAllByIdIn(final List<Long> ids) {
            return new ArrayList<>();
        }
    }

    private static final class FakeUserOnboardingAnswerRepository implements UserOnboardingAnswerRepositoryPort {
        private final Map<Long, List<UserOnboardingAnswer>> answersByUserId = new HashMap<>();

        @Override
        public List<UserOnboardingAnswer> findByUserId(final Long userId) {
            return answersByUserId.getOrDefault(userId, new ArrayList<>());
        }

        @Override
        public void deleteByUserIdAndOnboardingOptionIdIn(final Long userId, final List<Long> onboardingOptionIds) {}

        @Override
        public List<UserOnboardingAnswer> saveAll(final List<UserOnboardingAnswer> answers) {
            return answers;
        }
    }
}
