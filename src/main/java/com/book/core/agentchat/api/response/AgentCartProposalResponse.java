package com.book.core.agentchat.api.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AgentCartProposalResponse(Long proposalId, String status, Instant expiresAt, List<AgentCartItemResponse> items,
    Long executionId, Map<String, String> links) {
}
