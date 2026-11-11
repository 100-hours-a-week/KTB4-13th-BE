package com.book.core.preference.application.service;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPreferenceService {
    private static final int CONVERSATION_CHUNK_SESSIONS = 100;
    private static final long CONVERSATION_CHUNK_MESSAGES = 1_000;
    private static final int EXTRACTION_MAX_ATTEMPTS = 3;
    private static final long EXTRACTION_RETRY_DELAY_SECONDS = 2;

    @Value("${preference.nightly.concurrency:1}")
    private int nightlyConcurrency = 1;

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
        if (nightlyConcurrency < 1) {
            throw new IllegalArgumentException("preference.nightly.concurrency must be at least 1");
        }
        final Set<Long> memoryChangedUserIds = ConcurrentHashMap.newKeySet();
        final Set<Long> failedUserIds = ConcurrentHashMap.newKeySet();
        final List<List<MemoryExtractionTarget>> chunks = conversationChunks(getMemoryExtractionTargetsUseCase.execute());
        if (nightlyConcurrency == 1) {
            for (final List<MemoryExtractionTarget> chunk : chunks) {
                processTargets(chunk, conversations(chunk, failedUserIds), failedUserIds, memoryChangedUserIds);
            }
            return memoryChangedUserIds;
        }
        processChunksInParallel(chunks, failedUserIds, memoryChangedUserIds);
        return memoryChangedUserIds;
    }

    private void processChunksInParallel(final List<List<MemoryExtractionTarget>> chunks, final Set<Long> failedUserIds,
        final Set<Long> memoryChangedUserIds) {
        try (ExecutorService executor = Executors.newFixedThreadPool(nightlyConcurrency)) {
            for (final List<MemoryExtractionTarget> chunk : chunks) {
                processChunkInParallel(executor, chunk, failedUserIds, memoryChangedUserIds);
            }
        }
    }

    private void processChunkInParallel(final ExecutorService executor, final List<MemoryExtractionTarget> chunk,
        final Set<Long> failedUserIds, final Set<Long> memoryChangedUserIds) {
        final Map<Long, List<ChatHistoryMessage>> conversations = conversations(chunk, failedUserIds);
        final Map<Long, List<MemoryExtractionTarget>> targetsByUser = new LinkedHashMap<>();
        chunk.forEach(target -> targetsByUser.computeIfAbsent(target.userId(), ignored -> new ArrayList<>()).add(target));
        final AtomicReference<Error> taskError = new AtomicReference<>();
        final List<Callable<Void>> jobs = targetsByUser.values().stream().map(targets -> (Callable<Void>) () -> {
            try {
                processTargets(targets, conversations, failedUserIds, memoryChangedUserIds);
            } catch (final Error error) {
                taskError.compareAndSet(null, error);
            }
            return null;
        }).toList();
        awaitTasks(executor, jobs);
        if (taskError.get() != null) {
            throw taskError.get();
        }
    }

    private void awaitTasks(final ExecutorService executor, final List<Callable<Void>> jobs) {
        try {
            executor.invokeAll(jobs);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private void processTargets(final List<MemoryExtractionTarget> targets, final Map<Long, List<ChatHistoryMessage>> conversations,
        final Set<Long> failedUserIds, final Set<Long> memoryChangedUserIds) {
        for (final MemoryExtractionTarget target : targets) {
            if (failedUserIds.contains(target.userId())) {
                continue;
            }
            try {
                if (extractMemories(target, conversations.get(target.sessionId()))) {
                    memoryChangedUserIds.add(target.userId());
                }
            } catch (final RuntimeException exception) {
                failedUserIds.add(target.userId());
                log.atError().addKeyValue("userId", target.userId()).addKeyValue("sessionId", target.sessionId()).log(
                    "[USER_PREFERENCE] 대화 취향 기억 추출에 실패해 다음 밤에 다시 처리합니다. userId={}, sessionId={}, sequence={}, cause={}", target.userId(),
                    target.sessionId(), target.sequence(), cause(exception));
            }
        }
    }

    public void refreshNightlyProfiles(final Set<Long> memoryChangedUserIds) {
        final Set<Long> userIds = new TreeSet<>(memoryChangedUserIds);
        userIds.addAll(getProfileRefreshTargetsUseCase.execute());
        userIds.forEach(this::refreshProfile);
    }

    private Map<Long, List<ChatHistoryMessage>> conversations(final List<MemoryExtractionTarget> chunk, final Set<Long> failedUserIds) {
        try {
            return getChatHistoryUseCase.executeForMemoryExtraction(chunk, ExtractUserMemoriesUseCase.MAX_MESSAGES);
        } catch (final RuntimeException exception) {
            chunk.forEach((final MemoryExtractionTarget target) -> failedUserIds.add(target.userId()));
            log.error("[USER_PREFERENCE] 대화 기록 조회에 실패해 다음 밤에 다시 처리합니다. sessionCount={}, cause={}", chunk.size(), cause(exception));
            return Map.of();
        }
    }

    private boolean extractMemories(final MemoryExtractionTarget target, final List<ChatHistoryMessage> history) {
        if (!checkPersonalizationAgreementUseCase.execute(target.userId())) {
            return false;
        }
        if (history == null) {
            throw new CoreException(ErrorCode.CHAT_NOT_FOUND);
        }
        final List<ConversationMessage> messages =
            history.stream().map((final ChatHistoryMessage message) -> new ConversationMessage(message.role(), message.text())).toList();
        final ExtractUserMemoriesCommand extractCommand =
            new ExtractUserMemoriesCommand(target.userId(), String.valueOf(target.sessionId()), messages);
        final List<UserMemoryExtraction> extractions = extractWithRetry(extractCommand);
        return saveUserMemoriesUseCase
            .execute(new SaveUserMemoriesCommand(target.userId(), target.sessionId(), target.sequence(), extractions));
    }

    private List<UserMemoryExtraction> extractWithRetry(final ExtractUserMemoriesCommand command) {
        for (int attempt = 1;; attempt++) {
            try {
                return extractUserMemoriesUseCase.execute(command);
            } catch (final CoreException exception) {
                if (attempt >= EXTRACTION_MAX_ATTEMPTS || (exception.errorCode() != ErrorCode.AI_MEMORY_EXTRACTION_UNAVAILABLE
                    && exception.errorCode() != ErrorCode.AI_MEMORY_EXTRACTION_TIMEOUT)) {
                    throw exception;
                }
                waitFor(EXTRACTION_RETRY_DELAY_SECONDS);
            }
        }
    }

    private void refreshRecommendationFeed() {
        try {
            refreshRecommendationFeedUseCase.execute();
        } catch (final RuntimeException exception) {
            log.error("[USER_PREFERENCE] 추천 목록 다시 뽑기에 실패해 오늘 밤은 중단하고 다음 밤에 다시 부릅니다. cause={}", cause(exception));
        }
    }

    private void refreshProfile(final Long userId) {
        try {
            personalizationProfileUseCase.executeUpdate(userId);
        } catch (final RuntimeException exception) {
            log.atWarn().addKeyValue("userId", userId).log("[USER_PREFERENCE] AI 취향 프로필 갱신에 실패했습니다. userId={}, cause={}", userId,
                cause(exception));
        }
    }

    private static List<List<MemoryExtractionTarget>> conversationChunks(final List<MemoryExtractionTarget> targets) {
        final List<List<MemoryExtractionTarget>> chunks = new ArrayList<>();
        List<MemoryExtractionTarget> chunk = new ArrayList<>();
        long chunkMessages = 0;
        for (final MemoryExtractionTarget target : targets) {
            final long targetMessages = Math.min(target.sequence(), ExtractUserMemoriesUseCase.MAX_MESSAGES);
            if (!chunk.isEmpty()
                && (chunk.size() == CONVERSATION_CHUNK_SESSIONS || chunkMessages + targetMessages > CONVERSATION_CHUNK_MESSAGES)) {
                chunks.add(chunk);
                chunk = new ArrayList<>();
                chunkMessages = 0;
            }
            chunk.add(target);
            chunkMessages += targetMessages;
        }
        if (!chunk.isEmpty()) {
            chunks.add(chunk);
        }
        return chunks;
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
