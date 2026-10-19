package com.book.core.agentchat.application.result;

import java.util.List;

public record ChatSessionPageResult(List<ChatSessionSummary> sessions, Long nextBeforeId) {
    public ChatSessionPageResult {
        sessions = List.copyOf(sessions);
    }
}
