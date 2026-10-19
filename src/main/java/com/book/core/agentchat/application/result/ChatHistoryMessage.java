package com.book.core.agentchat.application.result;

import java.time.LocalDateTime;

public record ChatHistoryMessage(Long id, long sequence, String role, String text, LocalDateTime createdAt, ChatResponseSnapshot response) {
}
