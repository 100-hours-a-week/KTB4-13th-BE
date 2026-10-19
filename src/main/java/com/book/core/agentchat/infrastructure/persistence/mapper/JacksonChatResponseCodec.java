package com.book.core.agentchat.infrastructure.persistence.mapper;

import com.book.core.agentchat.application.port.ChatResponseCodec;
import com.book.core.agentchat.application.result.ChatResponseSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class JacksonChatResponseCodec implements ChatResponseCodec {
    private final ObjectMapper objectMapper;

    @Override
    public String encode(final ChatResponseSnapshot response) {
        return objectMapper.writeValueAsString(response);
    }

    @Override
    public ChatResponseSnapshot decode(final String payload) {
        return objectMapper.readValue(payload, ChatResponseSnapshot.class);
    }
}
