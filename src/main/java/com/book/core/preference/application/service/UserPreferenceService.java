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
import com.book.core.preference.application.command.ExtractChatMemoriesCommand;
import com.book.core.preference.application.result.UserPreferencesResult;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        for (final MemoryExtractionTarget target : getMemoryExtractionTargetsUseCase.execute()) {
            if (failedUserIds.contains(target.userId())) {
                continue;
            }
            try {
                extractMemories(new ExtractChatMemoriesCommand(target.userId(), target.sessionId(), target.sequence()));
            } catch (final RuntimeException exception) {
                // 같은 회원의 세션 순서를 지키도록 뒤 세션은 다음 밤으로 미룹니다.
                failedUserIds.add(target.userId());
                // 외부 응답 원문이 담길 수 있는 예외 메시지 대신 오류 코드나 예외 종류만 기록합니다.
                log.error("대화 취향 기억 추출에 실패해 다음 밤에 다시 처리합니다. userId={}, sessionId={}, sequence={}, cause={}", target.userId(),
                    target.sessionId(), target.sequence(), cause(exception));
            }
        }
    }

    public void extractMemories(final ExtractChatMemoriesCommand command) {
        if (!checkPersonalizationAgreementUseCase.execute(command.userId())) {
            return;
        }
        final List<ConversationMessage> messages = conversation(command);
        final ExtractUserMemoriesCommand extractCommand =
            new ExtractUserMemoriesCommand(command.userId(), String.valueOf(command.sessionId()), messages);
        List<UserMemoryExtraction> extractions;
        try {
            extractions = extractUserMemoriesUseCase.execute(extractCommand);
        } catch (final RetryAfterException exception) {
            waitFor(exception.retryAfterSeconds());
            extractions = extractUserMemoriesUseCase.execute(extractCommand);
        }
        saveUserMemoriesUseCase
            .execute(new SaveUserMemoriesCommand(command.userId(), command.sessionId(), command.sequence(), extractions));
    }

    private List<ConversationMessage> conversation(final ExtractChatMemoriesCommand command) {
        final List<ConversationMessage> messages = new ArrayList<>();
        long afterSequence = 0;
        while (afterSequence < command.sequence()) {
            final List<ChatHistoryMessage> page =
                getChatHistoryUseCase.execute(command.userId(), command.sessionId(), afterSequence, HISTORY_PAGE_SIZE);
            if (page.isEmpty()) {
                break;
            }
            page.stream().filter((final ChatHistoryMessage message) -> message.sequence() <= command.sequence())
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
