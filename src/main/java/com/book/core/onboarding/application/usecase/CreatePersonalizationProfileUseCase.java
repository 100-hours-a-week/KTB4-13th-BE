package com.book.core.onboarding.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.onboarding.application.port.AiPersonalizationProfileClient;
import com.book.core.onboarding.application.port.AiPersonalizationProfileRequest;
import com.book.core.onboarding.application.port.OnboardingOptionRepositoryPort;
import com.book.core.onboarding.application.port.TermRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingAnswerRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingBookRepositoryPort;
import com.book.core.onboarding.application.port.UserTermAgreementRepositoryPort;
import com.book.core.onboarding.domain.OnboardingOption;
import com.book.core.onboarding.domain.TermType;
import com.book.core.onboarding.domain.UserOnboardingAnswer;
import com.book.core.onboarding.domain.UserOnboardingBook;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreatePersonalizationProfileUseCase {
    private static final Long READING_TIME_QUESTION_ID = 1L;
    private static final Long CRITERIA_QUESTION_ID = 2L;
    private static final Long CATEGORY_QUESTION_ID = 3L;
    private static final Long TAG_QUESTION_ID = 4L;

    private final TermRepositoryPort termRepository;
    private final UserTermAgreementRepositoryPort agreementRepository;
    private final UserOnboardingAnswerRepositoryPort answerRepository;
    private final OnboardingOptionRepositoryPort optionRepository;
    private final UserOnboardingBookRepositoryPort bookRepository;
    private final AiPersonalizationProfileClient profileClient;

    public void execute(final Long userId) {
        final boolean agreed = termRepository.findActiveByTermType(TermType.PERSONALIZED_RECOMMENDATION)
            .map(term -> agreementRepository.existsAgreedByUserIdAndTermId(userId, term.id())).orElse(false);
        if (!agreed) {
            return;
        }

        final var answeredOptionIds = answerRepository.findByUserId(userId).stream().map(UserOnboardingAnswer::onboardingOptionId).toList();
        final var answeredOptions =
            optionRepository.findAllByIdIn(answeredOptionIds).stream().sorted(Comparator.comparing(OnboardingOption::id)).toList();
        final var likedBookIds = bookRepository.findByUserId(userId).stream().map(UserOnboardingBook::bookId).sorted().toList();
        profileClient.createProfile(new AiPersonalizationProfileRequest(userId, contentsOf(answeredOptions, READING_TIME_QUESTION_ID),
            contentsOf(answeredOptions, CRITERIA_QUESTION_ID), contentsOf(answeredOptions, CATEGORY_QUESTION_ID),
            contentsOf(answeredOptions, TAG_QUESTION_ID), likedBookIds));
    }

    private static List<String> contentsOf(final List<OnboardingOption> options, final Long questionId) {
        return options.stream().filter(option -> option.onboardingQuestionId().equals(questionId)).map(OnboardingOption::content).toList();
    }
}
