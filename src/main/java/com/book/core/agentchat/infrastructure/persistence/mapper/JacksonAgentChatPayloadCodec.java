package com.book.core.agentchat.infrastructure.persistence.mapper;

import com.book.core.agentchat.application.port.AgentChatPayloadCodec;
import com.book.core.agentchat.application.result.AgentChatContext;
import com.book.core.agentchat.application.result.AgentDecision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class JacksonAgentChatPayloadCodec implements AgentChatPayloadCodec {
    private final ObjectMapper mapper;

    public String encodeContext(final AgentChatContext context) {
        return mapper.writeValueAsString(context);
    }

    public AgentChatContext decodeContext(final String payload) {
        return mapper.readValue(payload, AgentChatContext.class);
    }

    public String encodeDecision(final AgentDecision decision) {
        return mapper.writeValueAsString(decision);
    }

    public AgentDecision decodeDecision(final String payload) {
        return mapper.readValue(payload, AgentDecision.class);
    }
}
