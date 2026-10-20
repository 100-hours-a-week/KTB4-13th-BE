package com.book.core.agentchat.application.service;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.exception.RetryAfterException;
import com.book.core.agentchat.application.command.GetChatHistoryCommand;
import com.book.core.agentchat.application.command.GetChatSessionsCommand;
import com.book.core.agentchat.application.command.SendChatMessageCommand;
import com.book.core.agentchat.application.result.AgentDecision;
import com.book.core.agentchat.application.result.AgentPreparedTurn;
import com.book.core.agentchat.application.result.ChatAttemptResult;
import com.book.core.agentchat.application.result.ChatHistoryMessage;
import com.book.core.agentchat.application.result.ChatResponseSnapshot;
import com.book.core.agentchat.application.result.ChatSessionPageResult;
import com.book.core.agentchat.application.usecase.BeginChatRequestUseCase;
import com.book.core.agentchat.application.usecase.CallAgentChatUseCase;
import com.book.core.agentchat.application.usecase.CompleteChatRequestUseCase;
import com.book.core.agentchat.application.usecase.CreateChatSessionUseCase;
import com.book.core.agentchat.application.usecase.FailChatRequestUseCase;
import com.book.core.agentchat.application.usecase.GetChatHistoryUseCase;
import com.book.core.agentchat.application.usecase.GetChatSessionsUseCase;
import com.book.core.agentchat.application.usecase.PrepareAgentTurnUseCase;
import com.book.core.recommendation.application.result.ChatRecommendationAiOutcome;
import com.book.core.recommendation.application.usecase.ChatRecommendationUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 대화방 HTTP와 내부 호출의 UseCase 실행 순서를 조율합니다. */
@Service
@RequiredArgsConstructor
public class ChatSessionService {
    private final CreateChatSessionUseCase create;
    private final GetChatHistoryUseCase history;
    private final GetChatSessionsUseCase getSessions;
    private final BeginChatRequestUseCase begin;
    private final ChatRecommendationUseCase recommend;
    private final CompleteChatRequestUseCase complete;
    private final FailChatRequestUseCase fail;
    private final PrepareAgentTurnUseCase prepareAgent;
    private final CallAgentChatUseCase agent;

    public Long create(final Long userId) {
        return create.execute(userId);
    }

    public ChatSessionPageResult list(final GetChatSessionsCommand command) {
        return getSessions.execute(command);
    }

    public List<ChatHistoryMessage> history(final GetChatHistoryCommand command) {
        return history.execute(command.userId(), command.sessionId(), command.afterSequence(), command.limit());
    }

    public ChatResponseSnapshot send(final SendChatMessageCommand command) {
        return finish(command, begin.execute(command));
    }

    private ChatResponseSnapshot finish(final SendChatMessageCommand command, final ChatAttemptResult attempt) {
        if (attempt.completedResponse() != null) {
            return attempt.completedResponse();
        }
        if (attempt.failure() != null) {
            if (attempt.retryAfterSeconds() != null) {
                throw new RetryAfterException(attempt.failure(), attempt.retryAfterSeconds());
            }
            throw new CoreException(attempt.failure());
        }
        if (attempt.agentPrepared() || agent.enabled()) {
            return finishAgent(command, attempt);
        }
        final ChatRecommendationAiOutcome outcome;
        try {
            outcome = recommend.execute(attempt.aiCommand());
        } catch (final RetryAfterException exception) {
            fail.execute(command, attempt.generation(), exception.errorCode(), exception.retryAfterSeconds());
            throw exception;
        } catch (final CoreException exception) {
            fail.execute(command, attempt.generation(), exception.errorCode());
            throw exception;
        }
        // 완료 저장/커밋 실패는 AI 실패로 기록하지 않고 같은 키의 복구 대상으로 남깁니다.
        return complete.execute(command, attempt.generation(), outcome);
    }

    private ChatResponseSnapshot finishAgent(final SendChatMessageCommand command, final ChatAttemptResult attempt) {
        final AgentPreparedTurn prepared = prepareAgent.execute(command, attempt.generation());
        AgentDecision decision = prepared.decision();
        if (decision == null) {
            if (!agent.enabled()) {
                throw new CoreException(ErrorCode.AI_RECOMMENDATION_UNAVAILABLE);
            }
            try {
                decision = agent.execute(attempt.aiCommand(), command.sessionId(), attempt.requestId(), prepared.context());
            } catch (final RetryAfterException exception) {
                fail.execute(command, attempt.generation(), exception.errorCode(), exception.retryAfterSeconds());
                throw exception;
            } catch (final CoreException exception) {
                fail.execute(command, attempt.generation(), exception.errorCode());
                throw exception;
            }
            decision = prepareAgent.saveDecision(command, attempt.generation(), decision);
        }
        return complete.executeAgent(command, attempt.generation(), decision, prepared.context(), attempt.aiCommand().spec());
    }

}
