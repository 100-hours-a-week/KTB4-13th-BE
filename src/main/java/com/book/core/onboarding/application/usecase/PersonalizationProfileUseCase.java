package com.book.core.onboarding.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.onboarding.application.port.AiPersonalizationProfileClient;
import com.book.core.onboarding.application.port.AiPersonalizationProfileRequest;
import com.book.core.onboarding.application.port.OnboardingOptionRepositoryPort;
import com.book.core.onboarding.application.port.ProfileHistoryRepositoryPort;
import com.book.core.onboarding.application.port.ProfileMemoryRepositoryPort;
import com.book.core.onboarding.application.port.ProfileSyncJobRepositoryPort;
import com.book.core.onboarding.application.port.TermRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingAnswerRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingBookRepositoryPort;
import com.book.core.onboarding.application.port.UserTermAgreementRepositoryPort;
import com.book.core.onboarding.domain.OnboardingOption;
import com.book.core.onboarding.domain.TermType;
import com.book.core.onboarding.domain.UserOnboardingAnswer;
import com.book.core.onboarding.domain.UserOnboardingBook;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class PersonalizationProfileUseCase {
    private static final Long READING_TIME_QUESTION_ID = 1L;
    private static final Long CRITERIA_QUESTION_ID = 2L;
    private static final Long CATEGORY_QUESTION_ID = 3L;
    private static final Long TAG_QUESTION_ID = 4L;
    private static final int MAX_MEMORIES = 500;

    private final TermRepositoryPort termRepository;
    private final UserTermAgreementRepositoryPort agreementRepository;
    private final UserOnboardingAnswerRepositoryPort answerRepository;
    private final OnboardingOptionRepositoryPort optionRepository;
    private final UserOnboardingBookRepositoryPort bookRepository;
    private final AiPersonalizationProfileClient profileClient;
    private final ProfileMemoryRepositoryPort memoryRepository;
    private final ProfileHistoryRepositoryPort historyRepository;
    private final ProfileSyncJobRepositoryPort profileJobs;
    private final Clock clock;

    public void executeUpdate(final Long userId) {
        update(userId, profileClient::updateProfile);
    }

    public void executeUpdate(final Long userId, final UUID revision) {
        update(userId, request -> profileClient.updateProfile(request, revision));
    }

    private void update(final Long userId, final Consumer<AiPersonalizationProfileRequest> sender) {
        final Optional<AiPersonalizationProfileRequest> request = profileRequestOf(userId);
        if (request.isEmpty()) {
            return;
        }
        sender.accept(request.orElseThrow());
        // 갱신 요청 중 동의가 없어졌다면 다른 삭제 작업이 이 갱신보다 먼저 처리됐을 수 있어 삭제를 다시 예약합니다.
        if (!agreed(userId)) {
            profileJobs.enqueue(userId, clock.instant());
        }
    }

    public void executeDelete(final Long userId) {
        profileClient.deleteProfile(userId);
    }

    public void executeDelete(final Long userId, final UUID revision) {
        profileClient.deleteProfile(userId, revision);
    }

    private boolean agreed(final Long userId) {
        return termRepository.findActiveByTermType(TermType.PERSONALIZED_RECOMMENDATION)
            .map(term -> agreementRepository.existsAgreedByUserIdAndTermId(userId, term.id())).orElse(false);
    }

    private Optional<AiPersonalizationProfileRequest> profileRequestOf(final Long userId) {
        if (!agreed(userId)) {
            return Optional.empty();
        }

        final var answeredOptionIds = answerRepository.findByUserId(userId).stream().map(UserOnboardingAnswer::onboardingOptionId).toList();
        final var answeredOptions =
            optionRepository.findAllByIdIn(answeredOptionIds).stream().sorted(Comparator.comparing(OnboardingOption::id)).toList();
        final var likedBookIds = bookRepository.findByUserId(userId).stream().map(UserOnboardingBook::bookId).sorted().toList();
        return Optional.of(new AiPersonalizationProfileRequest(userId, contentsOf(answeredOptions, READING_TIME_QUESTION_ID),
            contentsOf(answeredOptions, CRITERIA_QUESTION_ID), contentsOf(answeredOptions, CATEGORY_QUESTION_ID),
            contentsOf(answeredOptions, TAG_QUESTION_ID), likedBookIds, memoryRepository.findLatestActive(userId, MAX_MEMORIES),
            historyRepository.findHistory(userId)));
    }

    private static List<String> contentsOf(final List<OnboardingOption> options, final Long questionId) {
        return options.stream().filter(option -> option.onboardingQuestionId().equals(questionId)).map(OnboardingOption::content).toList();
    }
}
