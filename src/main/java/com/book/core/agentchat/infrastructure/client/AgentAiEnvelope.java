package com.book.core.agentchat.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record AgentAiEnvelope(AgentAiData data) {
}
