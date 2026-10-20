package com.book.core.agentchat.application.result;

import com.book.core.agentchat.domain.ChatSession;
import java.time.LocalDateTime;

public record ChatSessionSummary(Long sessionId, LocalDateTime createdAt) {
    public static ChatSessionSummary from(final ChatSession session) {
        return new ChatSessionSummary(session.id(), session.createdAt());
    }
}
