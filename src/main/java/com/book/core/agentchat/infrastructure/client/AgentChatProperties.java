package com.book.core.agentchat.infrastructure.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("agent-chat")
public record AgentChatProperties(boolean enabled) {
}
