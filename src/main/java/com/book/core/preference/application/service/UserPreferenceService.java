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
import com.book.core.preference.application.result.UserPreferencesResult;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPreferenceService {
    private static final int CONVERSATION_CHUNK_SESSIONS = 100;
    private static final long CONVERSATION_CHUNK_MESSAGES = 1_000;

    private final GetOnboardingProgressUseCase getOnboardingProgressUseCase;
    private final GetUserMemoriesUseCase getUserMemoriesUseCase;
    private final DeleteUserMemoriesUseCase deleteUserMemoriesUseCase;
    private final DeleteAllUserMemoriesUseCase deleteAllUserMemoriesUseCase;
    private final CheckPersonalizationAgreementUseCase checkPersonalizationAgreementUseCase;
    private final ExtractUserMemoriesUseCase extractUserMemoriesUseCase;
    private final SaveUserMemoriesUseCase saveUserMemoriesUseCase;
    private final GetChatHistoryUseCase getChatHistoryUseCase;
    private final GetMemoryExtractionTargetsUseCase getMemoryExtractionTargetsUseCase;

    public UserPreferencesResult getPreferences(final Long userId) {
        final OnboardingProgressResult onboarding = getCompletedOnboarding(userId);
        return new UserPreferencesResult(onboarding.answers(), getUserMemoriesUseCase.execute(userId));
    }

    public void deleteMemories(final DeleteUserMemoriesCommand command) {
        getCompletedOnboarding(command.userId());
        deleteUserMemoriesUseCase.execute(command);
    }

    public void deleteAllMemories(final Long userId) {
        getCompletedOnboarding(userId);
        deleteAllUserMemoriesUseCase.execute(userId);
    }

    public void extractNightlyMemories() {
        final Set<Long> failedUserIds = new HashSet<>();
        for (final List<MemoryExtractionTarget> chunk : conversationChunks(getMemoryExtractionTargetsUseCase.execute())) {
            final Map<Long, List<ChatHistoryMessage>> conversations = conversations(chunk, failedUserIds);
            for (final MemoryExtractionTarget target : chunk) {
                if (failedUserIds.contains(target.userId())) {
                    continue;
                }
                try {
                    extractMemories(target, conversations.get(target.sessionId()));
                } catch (final RuntimeException exception) {
                    failedUserIds.add(target.userId());
                    log.error("대화 취향 기억 추출에 실패해 다음 밤에 다시 처리합니다. userId={}, sessionId={}, sequence={}, cause={}", target.userId(),
                        target.sessionId(), target.sequence(), cause(exception));
                }
            }
        }
    }

    private Map<Long, List<ChatHistoryMessage>> conversations(final List<MemoryExtractionTarget> chunk, final Set<Long> failedUserIds) {
        try {
            return getChatHistoryUseCase.executeForMemoryExtraction(chunk, ExtractUserMemoriesUseCase.MAX_MESSAGES);
        } catch (final RuntimeException exception) {
            chunk.forEach((final MemoryExtractionTarget target) -> failedUserIds.add(target.userId()));
            log.error("대화 기록 조회에 실패해 다음 밤에 다시 처리합니다. sessionCount={}, cause={}", chunk.size(), cause(exception));
            return Map.of();
        }
    }

    private void extractMemories(final MemoryExtractionTarget target, final List<ChatHistoryMessage> history) {
        if (!checkPersonalizationAgreementUseCase.execute(target.userId())) {
            return;
        }
        if (history == null) {
            throw new CoreException(ErrorCode.CHAT_NOT_FOUND);
        }
        final List<ConversationMessage> messages =
            history.stream().map((final ChatHistoryMessage message) -> new ConversationMessage(message.role(), message.text())).toList();
        final ExtractUserMemoriesCommand extractCommand =
            new ExtractUserMemoriesCommand(target.userId(), String.valueOf(target.sessionId()), messages);
        List<UserMemoryExtraction> extractions;
        try {
            extractions = extractUserMemoriesUseCase.execute(extractCommand);
        } catch (final RetryAfterException exception) {
            waitFor(exception.retryAfterSeconds());
            extractions = extractUserMemoriesUseCase.execute(extractCommand);
        }
        saveUserMemoriesUseCase.execute(new SaveUserMemoriesCommand(target.userId(), target.sessionId(), target.sequence(), extractions));
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
