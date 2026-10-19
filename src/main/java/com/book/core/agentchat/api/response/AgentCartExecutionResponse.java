package com.book.core.agentchat.api.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AgentCartExecutionResponse(Long executionId, Long proposalId, Instant appliedAt, List<AgentCartItemResponse> items,
    Map<String, String> links) {
}
