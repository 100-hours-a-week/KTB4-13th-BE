package com.book.core.agentchat.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.agentchat.application.port.AgentChatClient;
import com.book.core.agentchat.application.result.AgentAction;
import com.book.core.agentchat.application.result.AgentChatContext;
import com.book.core.agentchat.application.result.AgentChosenBook;
import com.book.core.agentchat.application.result.AgentDecision;
import com.book.core.agentchat.application.result.AgentObservedProduct;
import com.book.core.recommendation.application.command.ChatRecommendationCommand;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CallAgentChatUseCase {
    private final AgentChatClient client;

    public boolean enabled() {
        return client.enabled();
    }

    public AgentDecision execute(final ChatRecommendationCommand command, final Long sessionId, final Long requestId,
        final AgentChatContext context) {
        final AgentDecision decision = client.chat(command, sessionId, requestId, context);
        decision.actions().forEach((final AgentAction action) -> context.requireProduct(action.isbn()));
        if (decision.selection() != null) {
            BigDecimal total = BigDecimal.ZERO;
            for (final AgentChosenBook chosen : decision.selection().chosen()) {
                final AgentObservedProduct observed = context.requireProduct(chosen.isbn());
                if (observed.price() == null || chosen.price().compareTo(observed.price()) != 0) {
                    throw new CoreException(ErrorCode.AI_SERVICE_FAILURE);
                }
                total = total.add(observed.price());
            }
            if ((decision.selection().budgetTotal() != null && total.compareTo(decision.selection().budgetTotal()) > 0)
                || (decision.selection().count() != null && decision.selection().count() != decision.selection().chosen().size())) {
                throw new CoreException(ErrorCode.AI_SERVICE_FAILURE);
            }
        }
        return decision;
    }
}
