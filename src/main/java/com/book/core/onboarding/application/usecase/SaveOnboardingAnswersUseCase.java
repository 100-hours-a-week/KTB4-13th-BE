package com.book.core.onboarding.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.onboarding.application.command.PutOnboardingAnswersCommand;
import com.book.core.onboarding.application.port.OnboardingOptionRepositoryPort;
import com.book.core.onboarding.application.port.OnboardingQuestionRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingAnswerRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingRepositoryPort;
import com.book.core.onboarding.domain.OnboardingOption;
import com.book.core.onboarding.domain.OnboardingQuestion;
import com.book.core.onboarding.domain.UserOnboarding;
import com.book.core.onboarding.domain.UserOnboardingAnswer;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class SaveOnboardingAnswersUseCase {
    private final OnboardingQuestionRepositoryPort questionRepository;
    private final OnboardingOptionRepositoryPort optionRepository;
    private final UserOnboardingAnswerRepositoryPort answerRepository;
    private final UserOnboardingRepositoryPort userOnboardingRepository;

    @Transactional
    public void execute(final PutOnboardingAnswersCommand command) {
        final OnboardingQuestion question = questionRepository
                .findById(command.questionId())
                .orElseThrow(() -> new CoreException(ErrorCode.ONBOARDING_QUESTION_NOT_FOUND));

        validateSelectionCount(question, command.optionIds());

        final List<OnboardingOption> selectedOptions = optionRepository.findAllByIdIn(command.optionIds());
        if (selectedOptions.size() != command.optionIds().size()) {
            throw new CoreException(ErrorCode.ONBOARDING_OPTION_NOT_FOUND);
        }
        final boolean mismatched = selectedOptions.stream()
                .anyMatch(option -> !option.onboardingQuestionId().equals(question.id()));
        if (mismatched) {
            throw new CoreException(ErrorCode.ONBOARDING_OPTION_QUESTION_MISMATCH);
        }

        validateParentAnswered(command.userId(), selectedOptions);

        ensureOnboardingStarted(command.userId());

        final List<Long> existingQuestionOptionIds = optionRepository.findByQuestionId(question.id()).stream()
                .map(OnboardingOption::id)
                .toList();
        answerRepository.deleteByUserIdAndOnboardingOptionIdIn(command.userId(), existingQuestionOptionIds);

        final List<UserOnboardingAnswer> newAnswers = command.optionIds().stream()
                .map(optionId -> UserOnboardingAnswer.of(command.userId(), optionId))
                .toList();
        answerRepository.saveAll(newAnswers);
    }

    private void validateSelectionCount(final OnboardingQuestion question, final List<Long> optionIds) {
        if (optionIds.size() < question.minSelection()) {
            throw new CoreException(ErrorCode.INVALID_ONBOARDING_ANSWER_SELECTION);
        }
        if (question.maxSelection() != null && optionIds.size() > question.maxSelection()) {
            throw new CoreException(ErrorCode.INVALID_ONBOARDING_ANSWER_SELECTION);
        }
    }

    private void validateParentAnswered(final Long userId, final List<OnboardingOption> selectedOptions) {
        final List<Long> parentOptionIds = selectedOptions.stream()
                .map(OnboardingOption::parentOptionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (parentOptionIds.isEmpty()) {
            return;
        }

        final Set<Long> answeredOptionIds = answerRepository.findByUserId(userId).stream()
                .map(UserOnboardingAnswer::onboardingOptionId)
                .collect(Collectors.toSet());
        final boolean parentMissing = parentOptionIds.stream().anyMatch(id -> !answeredOptionIds.contains(id));
        if (parentMissing) {
            throw new CoreException(ErrorCode.ONBOARDING_PARENT_QUESTION_NOT_ANSWERED);
        }
    }

    private void ensureOnboardingStarted(final Long userId) {
        if (userOnboardingRepository.findActiveByUserId(userId).isEmpty()) {
            userOnboardingRepository.save(UserOnboarding.start(userId));
        }
    }
}
