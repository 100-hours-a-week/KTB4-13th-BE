package com.book.core.onboarding.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.onboarding.application.port.OnboardingOptionRepositoryPort;
import com.book.core.onboarding.application.port.PersonalizationProfileClient;
import com.book.core.onboarding.application.port.PersonalizationProfileRequest;
import com.book.core.onboarding.application.port.UserConsentRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingAnswerRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingBookRepositoryPort;
import com.book.core.onboarding.domain.ConsentType;
import com.book.core.onboarding.domain.OnboardingOption;
import com.book.core.onboarding.domain.UserOnboardingAnswer;
import com.book.core.onboarding.domain.UserOnboardingBook;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * 온보딩 완료 후 개인화 추천 동의가 활성 상태인 사용자에 한해 AI 취향 프로필을 생성/갱신한다. DB 트랜잭션 밖에서 실행된다 — 호출부(OnboardingService)가
 * 별도 bean의 저장 UseCase 반환 뒤 이 UseCase를 부른다.
 *
 * Q4(tags)는 현재 DB에 저장된 답변을 그대로 사용한다. Q3 부모 답변이 나중에 바뀌어도 기존 Q4 답변이 정리되지 않는 정합성 문제는 BE #130 known
 * issue로 별도 관리하며 이 UseCase에서 수정하지 않는다.
 */
@UseCase
@RequiredArgsConstructor
public class CreatePersonalizationProfileUseCase {
    private static final Long READING_TIME_QUESTION_ID = 1L;
    private static final Long CRITERIA_QUESTION_ID = 2L;
    private static final Long CATEGORY_QUESTION_ID = 3L;
    private static final Long TAG_QUESTION_ID = 4L;
    private static final String IDEMPOTENCY_SCOPE = "profile";
    private static final String ITEM_SEPARATOR = "\u0001";
    private static final String FIELD_SEPARATOR = "\u0002";
    private static final String ALGORITHM = "SHA-256";

    private final UserConsentRepositoryPort userConsentRepository;
    private final UserOnboardingAnswerRepositoryPort answerRepository;
    private final OnboardingOptionRepositoryPort optionRepository;
    private final UserOnboardingBookRepositoryPort onboardingBookRepository;
    private final PersonalizationProfileClient personalizationProfileClient;

    public void execute(final Long userId) {
        if (userConsentRepository.findActiveByUserIdAndConsentType(userId, ConsentType.PERSONALIZED_RECOMMENDATION).isEmpty()) {
            return;
        }

        final List<UserOnboardingAnswer> answers = answerRepository.findByUserId(userId);
        final List<Long> answeredOptionIds = answers.stream().map(UserOnboardingAnswer::onboardingOptionId).toList();
        final Map<Long, OnboardingOption> optionById =
            optionRepository.findAllByIdIn(answeredOptionIds).stream().collect(Collectors.toMap(OnboardingOption::id, option -> option));

        final List<String> readingTimes = contentFor(answeredOptionIds, optionById, READING_TIME_QUESTION_ID);
        final List<String> criteria = contentFor(answeredOptionIds, optionById, CRITERIA_QUESTION_ID);
        final List<String> categories = contentFor(answeredOptionIds, optionById, CATEGORY_QUESTION_ID);
        final List<String> tags = contentFor(answeredOptionIds, optionById, TAG_QUESTION_ID);
        final List<Long> likedBookIds = onboardingBookRepository.findByUserId(userId).stream().map(UserOnboardingBook::bookId)
            .sorted(Comparator.naturalOrder()).toList();

        final String idempotencyKey = idempotencyKey(userId, readingTimes, criteria, categories, tags, likedBookIds);
        final PersonalizationProfileRequest request =
            new PersonalizationProfileRequest(userId, idempotencyKey, readingTimes, criteria, categories, tags, likedBookIds);
        personalizationProfileClient.createProfile(request);
    }

    private List<String> contentFor(final List<Long> answeredOptionIds, final Map<Long, OnboardingOption> optionById,
        final Long questionId) {
        return answeredOptionIds.stream().map(optionById::get).filter(Objects::nonNull)
            .filter(option -> option.onboardingQuestionId().equals(questionId)).sorted(Comparator.comparing(OnboardingOption::displayOrder))
            .map(OnboardingOption::content).toList();
    }

    private String idempotencyKey(final Long userId, final List<String> readingTimes, final List<String> criteria,
        final List<String> categories, final List<String> tags, final List<Long> likedBookIds) {
        final String likedBookIdsPart = likedBookIds.stream().sorted().map(String::valueOf).collect(Collectors.joining(ITEM_SEPARATOR));
        final String canonicalPayload =
            String.join(FIELD_SEPARATOR, String.join(ITEM_SEPARATOR, readingTimes), String.join(ITEM_SEPARATOR, criteria),
                String.join(ITEM_SEPARATOR, categories), String.join(ITEM_SEPARATOR, tags), likedBookIdsPart);
        try {
            final MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            final String hashHex = HexFormat.of().formatHex(digest.digest(canonicalPayload.getBytes(StandardCharsets.UTF_8)));
            return IDEMPOTENCY_SCOPE + ":" + userId + ":" + hashHex;
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 hash algorithm is unavailable.", exception);
        }
    }
}
