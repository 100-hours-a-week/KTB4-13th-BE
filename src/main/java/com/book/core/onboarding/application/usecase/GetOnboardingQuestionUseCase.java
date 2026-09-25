package com.book.core.onboarding.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.onboarding.application.command.GetOnboardingQuestionCommand;
import com.book.core.onboarding.application.port.OnboardingOptionRepositoryPort;
import com.book.core.onboarding.application.port.OnboardingQuestionRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingAnswerRepositoryPort;
import com.book.core.onboarding.application.result.OnboardingQuestionResult;
import com.book.core.onboarding.domain.OnboardingOption;
import com.book.core.onboarding.domain.OnboardingQuestion;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetOnboardingQuestionUseCase {
    private final OnboardingQuestionRepositoryPort questionRepository;
    private final OnboardingOptionRepositoryPort optionRepository;
    private final UserOnboardingAnswerRepositoryPort answerRepository;

    @Transactional(readOnly = true)
    public OnboardingQuestionResult execute(final GetOnboardingQuestionCommand command) {
        final OnboardingQuestion question = questionRepository
                .findById(command.questionId())
                .orElseThrow(() -> new CoreException(ErrorCode.ONBOARDING_QUESTION_NOT_FOUND));

        final List<OnboardingOption> options = resolveOptions(command.userId(), question);
        final Long nextQuestionId = questionRepository
                .findFirstByDisplayOrderGreaterThan(question.displayOrder())
                .map(OnboardingQuestion::id)
                .orElse(null);

        return OnboardingQuestionResult.of(question, options, nextQuestionId);
    }

    private List<OnboardingOption> resolveOptions(final Long userId, final OnboardingQuestion question) {
        final List<OnboardingOption> allOptions = optionRepository.findByQuestionId(question.id());

        final List<Long> parentOptionIds = allOptions.stream()
                .map(OnboardingOption::parentOptionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (parentOptionIds.isEmpty()) {
            return allOptions;
        }

        final Set<Long> answeredOptionIds = answerRepository.findByUserId(userId).stream()
                .map(answer -> answer.onboardingOptionId())
                .collect(Collectors.toSet());
        final Set<Long> matchedParentOptionIds =
                parentOptionIds.stream().filter(answeredOptionIds::contains).collect(Collectors.toSet());
        if (matchedParentOptionIds.isEmpty()) {
            throw new CoreException(ErrorCode.ONBOARDING_PARENT_QUESTION_NOT_ANSWERED);
        }

        return allOptions.stream()
                .filter(option -> matchedParentOptionIds.contains(option.parentOptionId()))
                .toList();
    }
}
