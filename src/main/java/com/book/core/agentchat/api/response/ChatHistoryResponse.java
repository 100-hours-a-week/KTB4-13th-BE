package com.book.core.agentchat.api.response;

import java.util.List;

public record ChatHistoryResponse(List<ChatHistoryMessageResponse> messages, Long nextAfterSequence) {
}
