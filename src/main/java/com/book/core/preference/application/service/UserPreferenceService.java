package com.book.core.preference.application.service;

import com.book.common.exception.CoreException;
import com.book.common.exception.RetryAfterException;
import com.book.core.agentchat.application.result.ChatHistoryMessage;
import com.book.core.agentchat.application.result.MemoryExtractionTarget;
import com.book.core.agentchat.application.usecase.GetChatHistoryUseCase;
import com.book.core.agentchat.application.usecase.GetMemoryExtractionTargetsUseCase;
import com.book.core.memory.application.command.ConversationMessage;
import com.book.core.memory.application.command.DeleteUserMemoriesCommand;
import com.book.core.memory.application.command.ExtractUserMemoriesCommand;
import com.book.core.memory.application.command.SaveUserMemoriesCommand;
import com.book.core.memory.application.command.UserMemoryExtraction;
import com.book.core.memory.application.usecase.DeleteAllUserMemoriesUseCase;
import com.book.core.memory.application.usecase.DeleteUserMemoriesUseCase;
import com.book.core.memory.application.usecase.ExtractUserMemoriesUseCase;
import com.book.core.memory.application.usecase.GetUserMemoriesUseCase;
import com.book.core.memory.application.usecase.SaveUserMemoriesUseCase;
import com.book.core.onboarding.application.command.GetOnboardingProgressCommand;
import com.book.core.onboarding.application.result.OnboardingProgressResult;
import com.book.core.onboarding.application.usecase.CheckPersonalizationAgreementUseCase;
import com.book.core.onboarding.application.usecase.GetOnboardingProgressUseCase;
import com.book.core.onboarding.application.usecase.GetProfileRefreshTargetsUseCase;
import com.book.core.onboarding.application.usecase.PersonalizationProfileUseCase;
import com.book.core.preference.application.result.UserPreferencesResult;
import com.book.core.recommendation.application.usecase.RefreshRecommendationFeedUseCase;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPreferenceService {
    private static final int HISTORY_PAGE_SIZE = 100;

    private final GetOnboardingProgressUseCase getOnboardingProgressUseCase;
    private final GetUserMemoriesUseCase getUserMemoriesUseCase;
    private final DeleteUserMemoriesUseCase deleteUserMemoriesUseCase;
    private final DeleteAllUserMemoriesUseCase deleteAllUserMemoriesUseCase;
    private final CheckPersonalizationAgreementUseCase checkPersonalizationAgreementUseCase;
    private final ExtractUserMemoriesUseCase extractUserMemoriesUseCase;
    private final SaveUserMemoriesUseCase saveUserMemoriesUseCase;
    private final GetChatHistoryUseCase getChatHistoryUseCase;
    private final GetMemoryExtractionTargetsUseCase getMemoryExtractionTargetsUseCase;
    private final PersonalizationProfileUseCase personalizationProfileUseCase;
    private final GetProfileRefreshTargetsUseCase getProfileRefreshTargetsUseCase;
    private final RefreshRecommendationFeedUseCase refreshRecommendationFeedUseCase;

    public UserPreferencesResult getPreferences(final Long userId) {
        final OnboardingProgressResult onboarding = getCompletedOnboarding(userId);
        return new UserPreferencesResult(onboarding.answers(), getUserMemoriesUseCase.execute(userId));
    }

    public void deleteMemories(final DeleteUserMemoriesCommand command) {
        getCompletedOnboarding(command.userId());
        if (deleteUserMemoriesUseCase.execute(command) > 0) {
            refreshProfile(command.userId());
        }
    }

    public void deleteAllMemories(final Long userId) {
        getCompletedOnboarding(userId);
        if (deleteAllUserMemoriesUseCase.execute(userId) > 0) {
            refreshProfile(userId);
        }
    }

    public void runNightlyBatch() {
        refreshNightlyProfiles(extractNightlyMemories());
        refreshRecommendationFeed();
    }

    public Set<Long> extractNightlyMemories() {
        final Set<Long> memoryChangedUserIds = new HashSet<>();
        final Set<Long> failedUserIds = new HashSet<>();
        for (final MemoryExtractionTarget target : getMemoryExtractionTargetsUseCase.execute()) {
            if (failedUserIds.contains(target.userId())) {
                continue;
            }
            try {
                if (extractMemories(target)) {
                    memoryChangedUserIds.add(target.userId());
                }
            } catch (final RuntimeException exception) {
                failedUserIds.add(target.userId());
                log.error("대화 취향 기억 추출에 실패해 다음 밤에 다시 처리합니다. userId={}, sessionId={}, sequence={}, cause={}", target.userId(),
                    target.sessionId(), target.sequence(), cause(exception));
            }
        }
        return memoryChangedUserIds;
    }

    public void refreshNightlyProfiles(final Set<Long> memoryChangedUserIds) {
        final Set<Long> userIds = new TreeSet<>(memoryChangedUserIds);
        userIds.addAll(getProfileRefreshTargetsUseCase.execute());
        userIds.forEach(this::refreshProfile);
    }

    private boolean extractMemories(final MemoryExtractionTarget target) {
        if (!checkPersonalizationAgreementUseCase.execute(target.userId())) {
            return false;
        }
        final List<ConversationMessage> messages = conversation(target);
        final ExtractUserMemoriesCommand extractCommand =
            new ExtractUserMemoriesCommand(target.userId(), String.valueOf(target.sessionId()), messages);
        List<UserMemoryExtraction> extractions;
        try {
            extractions = extractUserMemoriesUseCase.execute(extractCommand);
        } catch (final RetryAfterException exception) {
            waitFor(exception.retryAfterSeconds());
            extractions = extractUserMemoriesUseCase.execute(extractCommand);
        }
        return saveUserMemoriesUseCase
            .execute(new SaveUserMemoriesCommand(target.userId(), target.sessionId(), target.sequence(), extractions));
    }

    private void refreshRecommendationFeed() {
        try {
            refreshRecommendationFeedUseCase.execute();
        } catch (final RuntimeException exception) {
            log.error("추천 목록 다시 뽑기에 실패해 오늘 밤은 중단하고 다음 밤에 다시 부릅니다. cause={}", cause(exception));
        }
    }

    private void refreshProfile(final Long userId) {
        try {
            personalizationProfileUseCase.executeUpdate(userId);
        } catch (final RuntimeException exception) {
            log.warn("AI 취향 프로필 갱신에 실패했습니다. userId={}, cause={}", userId, cause(exception));
        }
    }

    private List<ConversationMessage> conversation(final MemoryExtractionTarget target) {
        final List<ConversationMessage> messages = new ArrayList<>();
        long afterSequence = 0;
        while (afterSequence < target.sequence()) {
            final List<ChatHistoryMessage> page =
                getChatHistoryUseCase.execute(target.userId(), target.sessionId(), afterSequence, HISTORY_PAGE_SIZE);
            if (page.isEmpty()) {
                break;
            }
            page.stream().filter((final ChatHistoryMessage message) -> message.sequence() <= target.sequence())
                .map((final ChatHistoryMessage message) -> new ConversationMessage(message.role(), message.text())).forEach(messages::add);
            afterSequence = page.getLast().sequence();
        }
        return messages;
    }

    private static void waitFor(final long seconds) {
        try {
            Thread.sleep(Duration.ofSeconds(seconds));
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private static String cause(final RuntimeException exception) {
        if (exception instanceof final CoreException coreException) {
            return coreException.errorCode().name();
        }
        if (exception instanceof final RetryAfterException retryAfterException) {
            return retryAfterException.errorCode().name();
        }
        return exception.getClass().getSimpleName();
    }

    private OnboardingProgressResult getCompletedOnboarding(final Long userId) {
        return getOnboardingProgressUseCase.executeCompleted(new GetOnboardingProgressCommand(userId));
    }
}
