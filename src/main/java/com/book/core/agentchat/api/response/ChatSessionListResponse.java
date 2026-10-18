package com.book.core.agentchat.api.response;

import java.util.List;

public record ChatSessionListResponse(List<ChatSessionSummaryResponse> sessions, Long nextBeforeId) {
}
