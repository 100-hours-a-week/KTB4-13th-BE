package com.book.core.agentchat.application.port;

import com.book.core.agentchat.application.result.AgentChatContext;
import com.book.core.agentchat.application.result.AgentDecision;

public interface AgentChatPayloadCodec {
    String encodeContext(AgentChatContext context);

    AgentChatContext decodeContext(String payload);

    String encodeDecision(AgentDecision decision);

    AgentDecision decodeDecision(String payload);
}
