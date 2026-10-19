package com.book.core.agentchat.api.response;

import java.time.LocalDateTime;

public record ChatSessionSummaryResponse(Long sessionId, LocalDateTime createdAt) {
}
