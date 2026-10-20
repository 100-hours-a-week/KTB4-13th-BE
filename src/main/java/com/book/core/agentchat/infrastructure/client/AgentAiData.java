package com.book.core.agentchat.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
record AgentAiData(String route, String reply, Map<String, Object> spec, List<AgentAiCard> cards, String followup,
    List<AgentAiButton> buttons, Boolean degraded, List<AgentAiAction> actions, AgentAiSelection selection) {
}
