package com.book.core.memory.application.port;

import com.book.core.memory.application.command.ConversationMessage;
import java.util.List;

public record AiMemoryExtractionRequest(Long userId, boolean consented, String conversationId, List<ConversationMessage> conversation) {
}
