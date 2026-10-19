package com.book.core.agentchat.api.response;

import com.book.core.agentchat.application.result.AgentButton;
import java.util.List;
import java.util.Map;

public record AgentRecommendationReplyResponse(String route, Map<String, Object> spec, String reply, List<ChatCardResponse> cards,
    String followup, List<AgentButton> buttons, boolean degraded) implements ChatReplyResponse {
}
