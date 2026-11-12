package com.book.core.agentchat.application.service;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.exception.RetryAfterException;
import com.book.core.agentchat.application.command.GetChatHistoryCommand;
import com.book.core.agentchat.application.command.GetChatSessionsCommand;
import com.book.core.agentchat.application.command.SendChatMessageCommand;
import com.book.core.agentchat.application.result.AgentAction;
import com.book.core.agentchat.application.result.AgentChatContext;
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
import com.book.core.book.application.usecase.GetAiBookIdentifiersUseCase;
import com.book.core.coupon.application.result.OwnedCouponResult;
import com.book.core.coupon.application.usecase.FindOwnedCouponsForCheckoutUseCase;
import com.book.core.point.application.usecase.GetPointBalanceUseCase;
import com.book.core.product.application.result.RecommendationProductResult;
import com.book.core.product.application.usecase.GetRecommendationProductsUseCase;
import com.book.core.recommendation.application.result.ChatRecommendationAiOutcome;
import com.book.core.recommendation.application.usecase.ChatRecommendationUseCase;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
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
    private final FindOwnedCouponsForCheckoutUseCase coupons;
    private final GetPointBalanceUseCase points;
    private final GetAiBookIdentifiersUseCase bookIdentifiers;
    private final GetRecommendationProductsUseCase products;
    private final Clock clock;

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
        return send(command, null);
    }

    public ChatResponseSnapshot send(final SendChatMessageCommand command, final Consumer<String> onDelta) {
        final ChatAttemptResult attempt = begin.execute(command);
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
            return finishAgent(command, attempt, onDelta);
        }
        final ChatRecommendationAiOutcome outcome;
        try {
            if (onDelta == null) {
                outcome = recommend.execute(attempt.aiCommand());
            } else {
                outcome = recommend.execute(attempt.aiCommand(), onDelta);
            }
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

    private ChatResponseSnapshot finishAgent(
        final SendChatMessageCommand command,
        final ChatAttemptResult attempt,
        final Consumer<String> onDelta
    ) {
        List<OwnedCouponResult> ownedCoupons = List.of();
        BigDecimal pointBalance = BigDecimal.ZERO;
        if (!attempt.agentPrepared()) {
            ownedCoupons = coupons.findUsable(command.userId(), LocalDateTime.now(clock));
            pointBalance = points.findOrZero(command.userId());
        }
        final AgentPreparedTurn prepared = prepareAgent.execute(command, attempt.generation(), ownedCoupons, pointBalance);
        AgentDecision decision = prepared.decision();
        if (decision == null) {
            if (!agent.enabled()) {
                throw new CoreException(ErrorCode.AI_RECOMMENDATION_UNAVAILABLE);
            }
            try {
                if (onDelta == null) {
                    decision = agent.execute(attempt.aiCommand(), command.sessionId(), attempt.requestId(), prepared.context());
                } else {
                    decision = agent.execute(attempt.aiCommand(), command.sessionId(), attempt.requestId(), prepared.context(), onDelta);
                }
            } catch (final RetryAfterException exception) {
                fail.execute(command, attempt.generation(), exception.errorCode(), exception.retryAfterSeconds());
                throw exception;
            } catch (final CoreException exception) {
                fail.execute(command, attempt.generation(), exception.errorCode());
                throw exception;
            }
            decision = prepareAgent.saveDecision(command, attempt.generation(), decision);
        }
        final Map<String, RecommendationProductResult> newProducts = findNewProducts(decision, prepared.context());
        return complete.executeAgent(command, attempt.generation(), decision, prepared.context(), newProducts, attempt.aiCommand().spec());
    }

    private Map<String, RecommendationProductResult> findNewProducts(final AgentDecision decision, final AgentChatContext context) {
        final List<String> newIsbns = decision.actions().stream().filter((final AgentAction action) -> "set".equals(action.op()))
            .map(AgentAction::isbn).filter((final String isbn) -> context.findProduct(isbn).isEmpty()).toList();
        if (newIsbns.isEmpty()) {
            return Map.of();
        }
        final Map<String, Long> bookIds = bookIdentifiers.findBookIds(newIsbns);
        final Map<Long, RecommendationProductResult> byBookId = products.execute(List.copyOf(bookIds.values()));
        final Map<String, RecommendationProductResult> byIsbn = new HashMap<>();
        bookIds.forEach((final String isbn, final Long bookId) -> {
            final RecommendationProductResult product = byBookId.get(bookId);
            if (product != null) {
                byIsbn.put(isbn, product);
            }
        });
        return byIsbn;
    }
}
