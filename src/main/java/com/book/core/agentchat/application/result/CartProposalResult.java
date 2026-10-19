package com.book.core.agentchat.application.result;

import com.book.core.agentchat.domain.CartChangeLine;
import java.time.Instant;
import java.util.List;

public record CartProposalResult(Long proposalId, String status, Instant expiresAt, List<CartChangeLine> items, Long executionId) {
    public CartProposalResult {
        items = List.copyOf(items);
    }
}
