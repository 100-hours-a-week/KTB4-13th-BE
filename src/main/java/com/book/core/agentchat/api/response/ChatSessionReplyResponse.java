package com.book.core.agentchat.api.response;

import java.util.List;
import java.util.Map;

public record ChatSessionReplyResponse(Map<String, Object> spec, String reply, List<ChatCardResponse> cards, String followup,
    List<String> buttons, boolean degraded) {
}
