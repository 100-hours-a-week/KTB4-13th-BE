package com.book.core.agentchat.application.port;

import com.book.core.agentchat.application.result.AgentChatContext;
import com.book.core.agentchat.application.result.AgentDecision;
import com.book.core.recommendation.application.command.ChatRecommendationCommand;

public interface AgentChatClient {
    boolean enabled();

    AgentDecision chat(ChatRecommendationCommand command, Long sessionId, Long requestId, AgentChatContext context);
}
