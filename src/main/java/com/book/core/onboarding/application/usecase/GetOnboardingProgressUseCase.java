package com.book.core.onboarding.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.onboarding.application.command.GetOnboardingProgressCommand;
import com.book.core.onboarding.application.port.OnboardingOptionRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingAnswerRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingBookRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingRepositoryPort;
import com.book.core.onboarding.application.result.OnboardingAnswerGroupResult;
import com.book.core.onboarding.application.result.OnboardingProgressResult;
import com.book.core.onboarding.domain.OnboardingOption;
import com.book.core.onboarding.domain.UserOnboarding;
import com.book.core.onboarding.domain.UserOnboardingAnswer;
import com.book.core.onboarding.domain.UserOnboardingBook;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetOnboardingProgressUseCase {
    private final UserOnboardingRepositoryPort userOnboardingRepository;
    private final UserOnboardingAnswerRepositoryPort answerRepository;
    private final OnboardingOptionRepositoryPort optionRepository;
    private final UserOnboardingBookRepositoryPort bookRepository;

    @Transactional(readOnly = true)
    public OnboardingProgressResult execute(final GetOnboardingProgressCommand command) {
        final UserOnboarding userOnboarding = userOnboardingRepository
                .findActiveByUserId(command.userId())
                .orElseThrow(() -> new CoreException(ErrorCode.ONBOARDING_NOT_FOUND));

        final List<UserOnboardingAnswer> answers = answerRepository.findByUserId(command.userId());
        final List<Long> answeredOptionIds =
                answers.stream().map(UserOnboardingAnswer::onboardingOptionId).toList();
        final List<OnboardingOption> options = optionRepository.findAllByIdIn(answeredOptionIds);
        final Map<Long, Long> optionIdToQuestionId = options.stream()
                .collect(Collectors.toMap(OnboardingOption::id, OnboardingOption::onboardingQuestionId));
        final Map<Long, Integer> optionIdToDisplayOrder =
                options.stream().collect(Collectors.toMap(OnboardingOption::id, OnboardingOption::displayOrder));

        final Map<Long, List<Long>> optionIdsByQuestionId = new TreeMap<>();
        for (final Long optionId : answeredOptionIds) {
            final Long questionId = optionIdToQuestionId.get(optionId);
            if (questionId == null) {
                continue;
            }
            optionIdsByQuestionId
                    .computeIfAbsent(questionId, key -> new ArrayList<>())
                    .add(optionId);
        }

        final List<OnboardingAnswerGroupResult> answerGroups = optionIdsByQuestionId.entrySet().stream()
                .map(entry -> new OnboardingAnswerGroupResult(
                        entry.getKey(),
                        entry.getValue().stream()
                                .sorted(Comparator.comparing(optionIdToDisplayOrder::get))
                                .toList()))
                .toList();

        final List<Long> bookIds = bookRepository.findByUserId(command.userId()).stream()
                .map(UserOnboardingBook::bookId)
                .sorted(Comparator.naturalOrder())
                .toList();

        return new OnboardingProgressResult(
                userOnboarding.onboardingStatus(), userOnboarding.completedAt(), answerGroups, bookIds);
    }
}
