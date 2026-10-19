package com.book.core.agentchat.api.response;

import java.time.LocalDateTime;

public record ChatHistoryMessageResponse(Long id, long sequence, String role, String text, LocalDateTime createdAt,
    ChatSessionReplyResponse response) {
}
