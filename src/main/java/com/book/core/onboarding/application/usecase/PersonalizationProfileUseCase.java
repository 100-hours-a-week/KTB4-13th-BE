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

    /**
     * 동의한 회원의 프로필을 갱신합니다. 갱신 중 동의가 철회되면 철회 쪽 삭제가 이 갱신보다 먼저 처리될 수 있으므로, 갱신 응답 뒤 동의를 다시 확인해 삭제 작업을 다시
     * 예약합니다. AI는 갱신을 응답 전에 저장하고 같은 회원의 갱신·삭제를 잠금으로 차례로 처리하므로 응답을 받은 갱신 뒤에는 삭제가 남습니다. 시간 초과로 결과를 모르는 갱신은
     * AI에서 늦게 처리될 수 있어 이 확인으로 막지 못합니다.
     */
    public void executeUpdate(final Long userId) {
        final Optional<AiPersonalizationProfileRequest> request = profileRequestOf(userId);
        if (request.isEmpty()) {
            return;
        }
        profileClient.updateProfile(request.orElseThrow());
        if (!agreed(userId)) {
            profileJobs.enqueue(userId, clock.instant());
        }
    }

    public void executeDelete(final Long userId) {
        profileClient.deleteProfile(userId);
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
